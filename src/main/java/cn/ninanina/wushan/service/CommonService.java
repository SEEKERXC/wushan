package cn.ninanina.wushan.service;

public interface CommonService {
    /**
     * <p>客户端请求生成appKey需要发送一个客户端生成的加密串secret，然后服务器解密，并检查secret的有效性。
     * <p>secret生成策略：首先获取当前UTC分钟数，然后连接一个指定密钥（比如"jdfohewk"）生成加密串，即为secret。
     * <p>加密算法：SHA128
     * <p>关于为什么不采用非对称加密，因为这不能保证是真实APP。谁都可以生成密钥对，谁都能保存私钥发布公钥，而发布出来的公钥则不一定是APP生成的。
     *
     * @return 解密串，无效则为空
     */
    boolean secretValid(String secret);

    /**
     * 根据secret生成appKey，作为app的唯一标识。
     * 虽然secret是根据分钟数生成的，但是appKey可以按照毫秒级来生成，并且是加了独占锁的。
     */
    String genAppkey(String secret);

    /**
     * 验证appKey有效性
     */
    boolean appKeyValid(String appKey);
}
