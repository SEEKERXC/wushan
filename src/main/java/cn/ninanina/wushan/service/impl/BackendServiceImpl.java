package cn.ninanina.wushan.service.impl;

import cn.ninanina.wushan.common.GoogleResult;
import cn.ninanina.wushan.common.util.LuceneUtil;
import cn.ninanina.wushan.domain.TagDetail;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.repository.TagRepository;
import cn.ninanina.wushan.repository.VideoRepository;
import cn.ninanina.wushan.service.BackendService;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PostConstruct;

/**
 * 执行一些后台任务，包括翻译、建索引、实时抓取视频链接等
 */
@Service
@Slf4j
public class BackendServiceImpl implements BackendService {
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private TagRepository tagRepository;

    //一个建索引线程，每个处理之间间隔500毫秒
    private final ScheduledExecutorService indexingExecutorService = Executors.newScheduledThreadPool(1);

    private static final String URL = "http://translate.google.cn/translate_a/single";

    //先不开启翻译，没钱了！！
    @PostConstruct
    public void init() {
//        startTranslate();
//        startIndexing();
    }

    @Override
    public void startTranslate() {
        log.info("started translating.");

        /* 翻译tag */
        new Thread(() -> {
            Long tagWatermark = tagRepository.findTranslateWaterMark();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (tagWatermark != null) {
                tagRepository.findById(tagWatermark).ifPresent(tagDetail -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("client", "gtx");
                    params.put("dt", "t");
                    params.put("dj", "1");
                    params.put("ie", "UTF-8");
                    params.put("sl", "auto");
                    params.put("tl", "zh");
                    String q = tagDetail.getTag();
                    params.put("q", q);
                    String tagZh = requestForHttp(params);
                    tagDetail.setTagZh(tagZh);
                    tagRepository.save(tagDetail);
                    log.info("translate tag id: {} tagEn: {} to tagZh: {}", tagDetail.getId(), tagDetail.getTag(), tagDetail.getTagZh());
                });
                tagWatermark = tagRepository.findTranslateWaterMark();
            }
        }).start();
        /* 翻译title */
        new Thread(() -> {
            Long watermark = videoRepository.findTranslateWatermark();
            while (watermark != null) {
                videoRepository.findById(watermark).ifPresent(videoDetail -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("client", "gtx");
                    params.put("dt", "t");
                    params.put("dj", "1");
                    params.put("ie", "UTF-8");
                    params.put("sl", "auto");
                    params.put("tl", "zh");
                    String q = videoDetail.getTitle();
                    if (StringUtils.isEmpty(q)) q = "no title";
                    params.put("q", q);
                    String titleZh = requestForHttp(params);
                    videoDetail.setTitleZh(titleZh);
                    videoRepository.save(videoDetail);
                    log.info("translate video id: {} titleEn: {} to titleZh: {}", videoDetail.getId(), videoDetail.getTitle(), videoDetail.getTitleZh());
                });
                watermark = videoRepository.findTranslateWatermark();
            }
        }).start();

    }

    @SneakyThrows
    @Override
    public void startIndexing() {
        log.info("started indexing");
        FSDirectory directory = LuceneUtil.get().getDirectory();
        Analyzer analyzer = LuceneUtil.get().getAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_4, analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        IndexWriter indexWriter = new IndexWriter(directory, config);
        AtomicLong count = new AtomicLong(1);
        indexingExecutorService.scheduleAtFixedRate(() -> {
            Long watermark = videoRepository.findIndexingWatermark();
            if (watermark != null) {
                VideoDetail videoDetail = videoRepository.getOne(watermark);
                Document document = new Document();
                document.add(new LongField("id", videoDetail.getId(), Field.Store.YES));
                document.add(new TextField("title", videoDetail.getTitle(), Field.Store.YES));
                document.add(new TextField("titleZh", videoDetail.getTitleZh(), Field.Store.YES));
                StringBuilder tagBuilder = new StringBuilder();
                //这里把已经翻译了的tag中文加进去
                StringBuilder tagZhBuilder = new StringBuilder();
                for (TagDetail tag : videoDetail.getTags()) {
                    tagBuilder.append(tag.getTag());
                    if (!StringUtils.isEmpty(tag.getTagZh())) {
                        tagZhBuilder.append(tag.getTagZh()).append(" ");
                    }
                }
                document.add(new TextField("tag", tagBuilder.toString(), Field.Store.YES));
                document.add(new TextField("tagZh", tagZhBuilder.toString(), Field.Store.YES));
                try {
                    indexWriter.addDocument(document);
                    indexWriter.commit();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                count.getAndIncrement();


                videoDetail.setIndexed(true);
                videoRepository.save(videoDetail);
                log.info("video {} is indexed.", videoDetail.getId());

                //每新增1000个，都重新读一次索引。
                if (count.get() % 1000 == 0) {
                    IndexReader reader = null;
                    try {
                        reader = DirectoryReader.open(LuceneUtil.get().getDirectory());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    LuceneUtil.get().setIndexReader(reader);
                    LuceneUtil.get().setIndexSearcher(new IndexSearcher(reader));
                    log.info("restart index reader and index searcher");
                }
            }
        }, 5000, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stopIndexing() {
        indexingExecutorService.shutdownNow();
    }

    @SneakyThrows
    private String requestForHttp(Map<String, String> params) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        /* httpPost */
        HttpPost httpPost = new HttpPost(URL);
        List<BasicNameValuePair> paramsList = new ArrayList<>();
        for (Map.Entry<String, String> en : params.entrySet()) {
            String key = en.getKey();
            String value = en.getValue();
            paramsList.add(new BasicNameValuePair(key, value));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(paramsList, "UTF-8"));
        log.info("translating {}", params.get("q"));
        CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
        try {
            /* 响应不是音频流，直接显示结果 */
            HttpEntity httpEntity = httpResponse.getEntity();
            String json = EntityUtils.toString(httpEntity, "UTF-8");
            EntityUtils.consume(httpEntity);
            Gson gson = new Gson();
            GoogleResult result = gson.fromJson(json, GoogleResult.class);
            return result.getSentences().get(0).getTrans();
        } finally {
            try {
                if (httpResponse != null) {
                    httpResponse.close();
                    httpClient.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
