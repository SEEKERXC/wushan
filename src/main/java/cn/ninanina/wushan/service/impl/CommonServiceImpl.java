package cn.ninanina.wushan.service.impl;

import cn.ninanina.wushan.common.Constant;
import cn.ninanina.wushan.common.util.EncodeUtil;
import cn.ninanina.wushan.domain.AppInfo;
import cn.ninanina.wushan.repository.AppInfoRepository;
import cn.ninanina.wushan.service.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommonServiceImpl implements CommonService {
    @Autowired
    private AppInfoRepository appInfoRepository;

    @Override
    public boolean secretValid(String secret) {
        long systemMillis = System.currentTimeMillis();
        log.info("verify secret: {}, current millis: {}", secret, systemMillis);
        long currentMinute = systemMillis / 1000 / 60;
        String v1 = EncodeUtil.encodeSHA(currentMinute + Constant.SECRET_KEY);
        if (secret.equals(v1)) return true;
        String v2 = EncodeUtil.encodeSHA((currentMinute - 1) + Constant.SECRET_KEY);
        if (secret.equals(v2)) return true;
        String v3 = EncodeUtil.encodeSHA((currentMinute + 1) + Constant.SECRET_KEY);
        return secret.equals(v3);
    }

    @Override
    public synchronized String genAppkey(String secret) {
        long systemMillis = System.currentTimeMillis();
        String appKey = EncodeUtil.encodeSHA(systemMillis + secret);
        while (appInfoRepository.findByAppKey(appKey) != null) {
            systemMillis = System.currentTimeMillis();
            appKey = EncodeUtil.encodeSHA(systemMillis + secret);
        }
        AppInfo appInfo = new AppInfo();
        appInfo.setAppKey(appKey);
        appInfo.setInstallTime(systemMillis);
        appInfoRepository.save(appInfo);
        return appKey;
    }

    @Override
    public boolean appKeyValid(String appKey) {
        return appInfoRepository.findByAppKey(appKey) != null;
    }
}
