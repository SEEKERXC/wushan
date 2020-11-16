package cn.ninanina.wushan.service.impl;

import cn.ninanina.wushan.common.Constant;
import cn.ninanina.wushan.common.Gender;
import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.domain.VideoDir;
import cn.ninanina.wushan.repository.UserRepository;
import cn.ninanina.wushan.repository.VideoDirRepository;
import cn.ninanina.wushan.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class UserServiceImp implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VideoDirRepository videoDirRepository;

    @Override
    public User register(String username, String password, String nickname, Gender gender) {
        User user = new User();
        Random random = new Random();
        int randInt = random.nextInt(Constant.USER_SCALE);
        user.setUsername(username);
        user.setPassword(password);
        if (!StringUtils.isEmpty(nickname)) user.setNickname(nickname);
        else user.setNickname("污友" + randInt);
        if (gender != null) user.setGender(gender);
        user.setRegisterTime(System.currentTimeMillis());
        user.setLastLoginTime(System.currentTimeMillis());
        user = userRepository.save(user);
        VideoDir videoDir = new VideoDir();
        videoDir.setName("默认收藏夹");
        videoDir.setCreateTime(System.currentTimeMillis());
        videoDir.setUpdateTime(System.currentTimeMillis());
        videoDir.setCount(0);
        videoDir.setUser(user);
        videoDirRepository.save(videoDir);
        return user;
    }

    @Override
    public User login(String username, String password) {
        User user = userRepository.findByUsernameAndPassword(username, password);
        user.setLastLoginTime(System.currentTimeMillis());
        userRepository.save(user);
        return user;
    }

}
