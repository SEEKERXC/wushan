package cn.ninanina.wushan.web;

import cn.ninanina.wushan.service.BackendService;
import cn.ninanina.wushan.service.CommonService;
import cn.ninanina.wushan.service.driver.DriverManager;
import cn.ninanina.wushan.web.result.Response;
import cn.ninanina.wushan.web.result.ResultMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    private DriverManager driverManager;

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


    @PostMapping("/driver")
    public Response registerDriver(@RequestParam("key") String key,
                                   @RequestParam("ip") String ip,
                                   @RequestParam("port") Integer port) {
        if (!key.equals("jdfohewk")) return result(ResultMsg.ParamError);
        boolean successful = driverManager.register(ip, port);
        return result(successful ? "成功" : "失败，请查看error日志");
    }
}
