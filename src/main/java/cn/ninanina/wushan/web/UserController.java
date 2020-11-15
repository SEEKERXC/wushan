package cn.ninanina.wushan.web;

import cn.ninanina.wushan.common.Constant;
import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.service.CommonService;
import cn.ninanina.wushan.service.UserService;
import cn.ninanina.wushan.web.result.Response;
import cn.ninanina.wushan.web.result.ResultMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController extends BaseController {
    @Autowired
    private UserService userService;
    @Autowired
    private CommonService commonService;

    @GetMapping("/ping")
    public Response ping() {
        return result("成功");
    }

    @PostMapping("/register")
    public Response register(@RequestParam("appKey") String appKey,
                             @RequestParam("password") String password) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        User user = userService.register(password);
        getSession().setAttribute(Constant.KEY_USER, user);
        log.info("ip {} registered a new user, id: {}", getIp(), user.getId());
        return result(user);
    }

    @PostMapping("/login")
    public Response login(@RequestParam("appKey") String appKey,
                          @RequestParam("UUID") Integer UUID,
                          @RequestParam("password") String password) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        User user = userService.login(UUID, password);
        if (user == null) {
            log.warn("login failed! ip addr: {}, UUID: {}", getIp(), UUID);
            return result(ResultMsg.FAILED, "用户名或密码错误");
        }
        getSession().setAttribute(Constant.KEY_USER, user);
        log.info("user {} logged in.", user.getId());
        return result(user);
    }

}
