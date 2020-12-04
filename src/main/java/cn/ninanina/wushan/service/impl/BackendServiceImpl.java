package cn.ninanina.wushan.service.impl;

import cn.ninanina.wushan.common.GoogleResult;
import cn.ninanina.wushan.common.util.CommonUtil;
import cn.ninanina.wushan.common.util.LuceneUtil;
import cn.ninanina.wushan.domain.TagDetail;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.repository.TagRepository;
import cn.ninanina.wushan.repository.VideoRepository;
import cn.ninanina.wushan.service.BackendService;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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

    //视频建索引线程池
    private final ScheduledExecutorService indexingExecutorService = Executors.newScheduledThreadPool(1);

    private static final String URL = "http://translate.google.cn/translate_a/single";

    @PostConstruct
    public void init() {
        startTranslate();
        startIndexing();
        getPinyinForTags();
        indexingTags();
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
                    if (!StringUtils.isEmpty(videoDetail.getTitleZh())) return;
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
        IndexWriter indexWriter = LuceneUtil.get().getIndexWriter();
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

    //对翻译了的tag解析拼音首字母并保存，都采用小写
    private void getPinyinForTags() {
        new Thread(() -> {
            long pinyinWatermark = tagRepository.findPinyinWaterMark();
            long translateWatermark = tagRepository.findTranslateWaterMark();
            log.info("start pinyin program.");
            for (long i = pinyinWatermark; i < translateWatermark; i++) {
                tagRepository.findById(i).ifPresent(tagDetail -> {
                    char start;
                    if (StringUtils.isEmpty(tagDetail.getTagZh())) return;
                    char word = tagDetail.getTagZh().charAt(0);
                    // 先判断其是否是汉字
                    if (String.valueOf(word).matches("[\\u4E00-\\u9FA5]+")) {
                        //提取汉字的首字母
                        String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(word);
                        if (pinyinArray != null) {
                            start = pinyinArray[0].charAt(0);
                        } else {
                            start = word;
                        }
                    } else start = word;
                    tagDetail.setStart(start);
                    tagRepository.save(tagDetail);
                    log.info("save tagZh {} start pinyin {}", tagDetail.getTagZh(), start);
                });
            }
            log.info("pinyin program ended.");
        }).start();
    }

    /**
     * 给翻译过的标签建索引，方便搜素。索引内容应包含id、英文内容、中文内容、中文拼音。
     */
    @SneakyThrows
    private void indexingTags() {
        log.info("start indexing tags");
        IndexWriter indexWriter = LuceneUtil.get().getTagIndexWriter();
        new Thread(() -> {
            long translateWatermark = tagRepository.findTranslateWaterMark();
            long indexingWatermark = tagRepository.findIndexingWatermark();
            for (long i = indexingWatermark; i < translateWatermark; i++) {
                tagRepository.findById(i).ifPresent(tagDetail -> {
                    String pinyin = CommonUtil.getPinyin(tagDetail.getTagZh());
                    Document document = new Document();
                    document.add(new LongField("id", tagDetail.getId(), Field.Store.YES));
                    document.add(new TextField("tag", tagDetail.getTag(), Field.Store.YES));
                    document.add(new TextField("tagZh", tagDetail.getTagZh(), Field.Store.YES));
                    document.add(new TextField("pinyin", pinyin, Field.Store.YES));
                    try {
                        indexWriter.addDocument(document);
                        indexWriter.commit();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    tagDetail.setIndexed(true);
                    tagRepository.save(tagDetail);
                    log.info("tag {} is indexed, tagZh {}, pinyin {}", tagDetail.getId(), tagDetail.getTagZh(), pinyin);
                });
            }
            log.info("indexing tags finished.");
        }).start();
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
