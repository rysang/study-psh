package springbook.user.dao;

import java.util.Map;

public class SimpleSqlService implements SqlService {

	private Map<String, String> sqlMap;
	public void setSqlMap(Map<String, String> sqlMap) {
		this.sqlMap = sqlMap;
	}
	
	public String getSql(String key) throws SqlRetrievalFailureException {
		String sql = sqlMap.get(key);
		if(sql == null)
			throw new SqlRetrievalFailureException(key + "�� ���� SQL �� ã�� �� ����");
		else 
			return sql;
	}

	
}
