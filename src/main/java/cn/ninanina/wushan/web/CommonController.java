package cn.ninanina.wushan.web;

import cn.ninanina.wushan.service.BackendService;
import cn.ninanina.wushan.service.CommonService;
import cn.ninanina.wushan.service.cache.DownloadManager;
import cn.ninanina.wushan.web.result.Response;
import cn.ninanina.wushan.web.result.ResultMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;

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
     * 删除错误cookie
     */
    @PostMapping("/cookie/delete")
    public Response deleteCookie(@RequestParam("key") String key,
                                 @RequestParam("email") String email) {
        if (!key.equals("jdfohewk")) return result(ResultMsg.ParamError);
        return result(downloadManager.deleteCookie(email));
    }
    //TODO:提供更加直观的cookie管理
    //todo:长期要做的：根据请求量动态控制cookie数量
}
