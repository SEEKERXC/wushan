package cn.ninanina.wushan.service.impl;

import cn.ninanina.wushan.common.Constant;
import cn.ninanina.wushan.common.Gender;
import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.domain.Playlist;
import cn.ninanina.wushan.repository.UserRepository;
import cn.ninanina.wushan.repository.PlaylistRepository;
import cn.ninanina.wushan.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class UserServiceImp implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlaylistRepository playlistRepository;

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
        Playlist playlist = new Playlist();
        playlist.setName("默认收藏夹");
        playlist.setCreateTime(System.currentTimeMillis());
        playlist.setUpdateTime(System.currentTimeMillis());
        playlist.setIsPublic(true);
        playlist.setCount(0);
        playlist.setUser(user);
        playlistRepository.save(playlist);
        return user;
    }

    @Override
    public User login(String username, String password) {
        User user = userRepository.findByUsernameAndPassword(username, password);
        log.info("user login, username {}, password {}, result {}", username, password, user == null ? "failed" : "success:" + user.getId());
        if (user == null) return null;
        user.setLastLoginTime(System.currentTimeMillis());
        userRepository.save(user);
        return user;
    }

}
