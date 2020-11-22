package cn.ninanina.wushan.web;

import cn.ninanina.wushan.common.Gender;
import cn.ninanina.wushan.common.util.EncodeUtil;
import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.repository.UserRepository;
import cn.ninanina.wushan.service.CommonService;
import cn.ninanina.wushan.service.UserService;
import cn.ninanina.wushan.web.cache.UserCacheManager;
import cn.ninanina.wushan.web.result.Response;
import cn.ninanina.wushan.web.result.ResultMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.*;

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
    @Autowired
    private UserCacheManager userCacheManager;

    @PostMapping("/register")
    public Response register(@RequestParam("appKey") String appKey,
                             @RequestParam("username") String username,
                             @RequestParam("password") String password,
                             String nickname,
                             Gender gender) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (userRepository.findByUsername(username) != null) return result(ResultMsg.USER_EXIST);
        User user = userService.register(username, password, nickname, gender);
        String token = genToken(appKey, username);
        userCacheManager.save(token, user);
        log.info("ip {} registered a new user, id: {}", getIp(), user.getId());
        return result(Pair.of(token, user));
    }

    @PostMapping("/login")
    public Response login(@RequestParam("appKey") String appKey,
                          @RequestParam("username") String username,
                          @RequestParam("password") String password) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        User user = userService.login(username, password);
        if (user == null) {
            return result(ResultMsg.FAILED);
        }
        String token = genToken(appKey, username);
        userCacheManager.save(token, user);
        return result(Pair.of(token, user));
    }

    @PostMapping("/logout")
    public Response logout(@RequestParam("appKey") String appKey,
                           @RequestParam("token") String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (getUser(token) == null) return result(ResultMsg.NOT_LOGIN);
        userCacheManager.delete(token);
        log.info("user {} logged out", getUser(token).getId());
        return result();
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
    public Response loggedIn(@RequestParam("appKey") String appKey,
                             @RequestParam("token") String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        User user = getUser(token);
        log.info("check for token {}, result {}", token, user == null ? "failed" : "success:" + user.getId());
        if (user == null) return result(ResultMsg.NOT_LOGIN);
        else return result(user);
    }

    /**
     * 生成用户token
     */
    private String genToken(String appKey, String username) {
        String currentMillis = String.valueOf(System.currentTimeMillis());
        String token = EncodeUtil.encodeSHA(appKey + username + currentMillis);
        while (userCacheManager.get(token) != null) {
            token = EncodeUtil.encodeSHA(appKey + username + currentMillis);
        }
        return token;
    }

}
