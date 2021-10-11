package cn.ninanina.wushan.web;

import cn.ninanina.wushan.domain.Feedback;
import cn.ninanina.wushan.domain.VersionInfo;
import cn.ninanina.wushan.repository.FeedbackRepository;
import cn.ninanina.wushan.repository.UserRepository;
import cn.ninanina.wushan.repository.VersionRepository;
import cn.ninanina.wushan.service.BackendService;
import cn.ninanina.wushan.service.CommonService;
import cn.ninanina.wushan.service.cache.DownloadManager;
import cn.ninanina.wushan.web.result.Response;
import cn.ninanina.wushan.web.result.ResultMsg;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController extends BaseController implements ApplicationContextAware {
    private ApplicationContext context;

    @Autowired
    private BackendService backendService;

    @Autowired
    private CommonService commonService;

    @Autowired
    private DownloadManager downloadManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @PostMapping("/shutdown")
    public Response shutdown(@RequestParam("apiKey") String apiKey) {
        if (StringUtils.equals(apiKey, "jdfohewk")) {
            ((ConfigurableApplicationContext) context).close();
            log.warn("Application closed successfully, remote ip:{}", getIp());
            return result("Application closed successfully.");
        }
        return result(ResultMsg.ParamError, "Application closed failed, wrong apiKey!");
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @PostMapping("/stop/indexing")
    public Response stopIndexing(@RequestParam("apiKey") String apiKey) {
        if (StringUtils.equals(apiKey, "jdfohewk")) {
            backendService.stopIndexing();
            log.warn("Indexing stopped, remote ip: {}", getIp());
            return result("Indexing stopped.");
        }
        return result(ResultMsg.ParamError, "Indexing stopped failed, wrong apiKey!");
    }

    @PostMapping("/genAppkey")
    public Response genAppkey(@RequestParam("secret") String secret) {
        if (!commonService.secretValid(secret)) {
            log.warn("secret {} is not valid.", secret);
            return result(ResultMsg.SECRET_INVALID);
        }
        String appKey = commonService.genAppkey(secret);
        log.info("appKey generated, ip: {}, appKey: {}", getIp(), appKey);
        return result(appKey);
    }

    /**
     * 添加cookie，只需要session_token就足够了
     */
    @PostMapping("/cookie")
    public Response registerCookie(@RequestParam("key") String key,
                                   @RequestParam("email") String email,
                                   @RequestParam("cookie") String cookie) {
        if (!key.equals("jdfohewk")) return result(ResultMsg.ParamError);
        int size = downloadManager.addCookie(email, cookie);
        return result("队列当前大小：" + size);
    }

    /**
     * 获取当前空闲的cookies
     */
    @GetMapping("/cookie/all")
    public Response allCookies(@RequestParam("key") String key) {
        if (!key.equals("jdfohewk")) return result(ResultMsg.ParamError);
        return result(downloadManager.getCookies(null, null));
    }

    /**
     * 查看当前队列中cookie总数
     */
    @GetMapping("/cookie/count")
    public Response cookieCount(@RequestParam("key") String key) {
        if (!key.equals("jdfohewk")) return result(ResultMsg.ParamError);
        return result(downloadManager.cookieCount());
    }

    /**
     * 删除错误cookie
     */
    @PostMapping("/cookie/delete")
    public Response deleteCookie(@RequestParam("key") String key,
                                 @RequestParam("email") String email) {
        if (!key.equals("jdfohewk")) return result(ResultMsg.ParamError);
        return result(downloadManager.deleteCookie(email));
    }
    //TODO:提供更加直观的cookie管理

    /**
     * 从txt文件一次性读取cookies，并且替换掉原本的cookies
     */
//    @SneakyThrows
//    @PostMapping("/cookie/refresh")
//    public Response refreshCookies(@RequestParam("key") String key){
//        if (!key.equals("jdfohewk")) return result(ResultMsg.ParamError);
//        ClassPathResource cpr = new ClassPathResource("static/token.txt");
//        byte[] keywordsData = FileCopyUtils.copyToByteArray(cpr.getInputStream());
//        String[] strs = new String(keywordsData, StandardCharsets.UTF_8).split(" ");
//    }

    /**
     * 发布版本信息
     */
    @PostMapping("/version/publish")
    public Response publishVersion(@RequestParam("key") String key,
                                   @RequestParam("code") String code,
                                   @RequestParam("info") String info,
                                   @RequestParam("url") String url) {
        if (!key.equals("jdfohewk")) return result(ResultMsg.ParamError);
        VersionInfo versionInfo = new VersionInfo();
        versionInfo.setVersionCode(code);
        versionInfo.setUpdateInfo(info);
        versionInfo.setAppUrl(url);
        versionInfo.setUpdateTime(System.currentTimeMillis());
        versionInfo = versionRepository.save(versionInfo);
        log.info("publish a new version: {}", code);
        return result(versionInfo);
    }

    /**
     * 获取当前最新版本信息
     */
    @GetMapping("/version")
    public Response newVersion() {
        return result(versionRepository.getLatestVersion());
    }

    /**
     * 提交反馈信息
     */
    @PostMapping("/feedback")
    public Response feedback(@RequestParam("appKey") String appKey,
                             @RequestParam("token") String token,
                             @RequestParam("content") String content) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        if (StringUtils.isEmpty(content)) return result(ResultMsg.EMPTY_CONTENT);
        Feedback feedback = new Feedback();
        feedback.setUser(userRepository.getOne(userId));
        feedback.setContent(content);
        feedback.setTime(System.currentTimeMillis());
        feedback = feedbackRepository.saveAndFlush(feedback);
        return result(feedback);
    }

    /**
     * 获取联系方式
     */
    @GetMapping("/contact")
    public Response contact(@RequestParam("appKey") String appKey) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        return result("AStupidMan100@gmail.com");
    }

    /**
     * 获取用户协议
     */
    @SneakyThrows
    @GetMapping("/protocol")
    public Response getProtocol(@RequestParam("appKey") String appKey) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        ClassPathResource cpr = new ClassPathResource("static/protocol.html");
        byte[] keywordsData = FileCopyUtils.copyToByteArray(cpr.getInputStream());
        String result = new String(keywordsData, StandardCharsets.UTF_8);
        return result(result);
    }


    @SneakyThrows
    @PostMapping("/upload")
    public Response fileUpload(@RequestParam("file") MultipartFile srcFile) {
        //前端没有选择文件，srcFile为空
        if (srcFile.isEmpty()) {
            return result(ResultMsg.ParamError);
        }
        //选择了文件，开始上传操作
        try {
            //构建上传目标路径，找到了项目的target的classes目录
            File destFile = new File("/home/data/");
            if (!destFile.exists()) {
                destFile = new File("");
            }
            //输出目标文件的绝对路径
            System.out.println("file path:" + destFile.getAbsolutePath());
            //拼接子路径
            SimpleDateFormat sf_ = new SimpleDateFormat("yyyyMMddHHmmss");
            String times = sf_.format(new Date());
            File upload = new File(destFile.getAbsolutePath(), "picture/" + times);
            //若目标文件夹不存在，则创建
            if (!upload.exists()) {
                upload.mkdirs();
            }
            System.out.println("完整的上传路径：" + upload.getAbsolutePath() + "/" + srcFile);
            //根据srcFile大小，准备一个字节数组
            byte[] bytes = srcFile.getBytes();
            //拼接上传路径
            //Path path = Paths.get(UPLOAD_FOLDER + srcFile.getOriginalFilename());
            //通过项目路径，拼接上传路径
            Path path = Paths.get(upload.getAbsolutePath() + "/" + srcFile.getOriginalFilename());
            //** 开始将源文件写入目标地址
            Files.write(path, bytes);
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            // 获得文件原始名称
            String fileName = srcFile.getOriginalFilename();
            // 获得文件后缀名称
            String suffixName = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            // 生成最新的uuid文件名称
            String newFileName = uuid + "." + suffixName;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result();
    }

    @GetMapping("/apk/{name}")
    public ResponseEntity<byte[]> download(@PathVariable("name") String name) throws IOException {
        File file = new File("/home" + File.separator + "apk" + File.separator + name);
        if (file.exists()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", file.getName());
            return new ResponseEntity<>(FileUtils.readFileToByteArray(file), headers, HttpStatus.OK);
        } else {
            System.out.println("文件不存在,请重试...");
            return null;
        }
    }
}
