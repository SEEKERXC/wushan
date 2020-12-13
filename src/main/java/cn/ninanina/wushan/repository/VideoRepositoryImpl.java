package cn.ninanina.wushan.repository;

import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Repository
public class VideoRepositoryImpl {
    @PersistenceContext
    private EntityManager entityManager;

    public List<Long> findLimitedInIdsWithOrder(List<Long> ids, String sort, int offset, int limit) {
        StringBuilder idStr = new StringBuilder("(");
        for (int i = 0; i < ids.size(); i++) {
            idStr.append(ids.get(i).longValue());
            if (i != ids.size() - 1) idStr.append(',');
        }
        idStr.append(')');
        String sql = "select id from video where id in " +
                idStr.toString() + " order by " + sort + " desc limit " + offset + ", " + limit;
        Query query = entityManager.createNativeQuery(sql);
        List<BigInteger> bigIntegers = query.getResultList();
        List<Long> result = new ArrayList<>();
        for (BigInteger bigInteger : bigIntegers) result.add(bigInteger.longValue());
        return result;
    }
}
