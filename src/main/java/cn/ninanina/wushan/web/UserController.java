package cn.ninanina.wushan.web;

import cn.ninanina.wushan.common.Constant;
import cn.ninanina.wushan.common.Gender;
import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.repository.UserRepository;
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
    private UserRepository userRepository;
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
                             @RequestParam("username") String username,
                             @RequestParam("password") String password,
                             String nickname,
                             Gender gender) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (userRepository.findByUsername(username) != null) return result(ResultMsg.USER_EXIST);
        User user = userService.register(username, password, nickname, gender);
        getSession().setAttribute(Constant.KEY_USER, user);
        log.info("ip {} registered a new user, id: {}", getIp(), user.getId());
        return result(user);
    }

    @PostMapping("/login")
    public Response login(@RequestParam("appKey") String appKey,
                          @RequestParam("username") String username,
                          @RequestParam("password") String password) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        User user = userService.login(username, password);
        if (user == null) {
            log.warn("login failed! ip addr: {}, username: {}", getIp(), user);
            return result(ResultMsg.FAILED);
        }
        getSession().setAttribute(Constant.KEY_USER, user);
        log.info("user {} logged in.", user.getId());
        return result(user);
    }

    @GetMapping("/exist")
    public Response exist(@RequestParam("appKey") String appKey,
                          @RequestParam("username") String username) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (userRepository.findByUsername(username) != null) return result(ResultMsg.USER_EXIST, "用户存在");
        else return result("用户不存在");
    }

    /**
     * 检查是否登录
     *
     * @param appKey appKey
     * @return 已登录返回SUCCESS，未登录返回NOT_LOGIN
     */
    @GetMapping("/loggedIn")
    public Response loggedIn(@RequestParam("appKey") String appKey) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        User user = getUser();
        if (user == null) return result(ResultMsg.NOT_LOGIN);
        else return result(user);
    }

}
