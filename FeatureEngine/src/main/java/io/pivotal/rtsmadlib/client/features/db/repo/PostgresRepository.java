/**********************************************************************************************
   Copyright 2019 Pivotal Software

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 *********************************************************************************************/

package io.pivotal.rtsmadlib.client.features.db.repo;

import java.sql.Array;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import io.pivotal.rtsmadlib.client.features.meta.ApplicationProperties;

/**
 * @author sridhar paladugu
 *
 */
@Repository
public class PostgresRepository {
	static final Log log = LogFactory.getLog(PostgresRepository.class.getName());

	@Autowired
	ApplicationProperties appProps;
	
	@Autowired
	@Qualifier("containerdbJdbcTemplate")
	JdbcTemplate containerdbJdbcTemplate;
	
	@Autowired
	@Qualifier("containerdbJdbcTemplate")
	NamedParameterJdbcTemplate containerdbNPJdbcTemplate;

	public void runDDL(String ddl) {
		containerdbJdbcTemplate.execute(ddl);
	}

	public SimpleJdbcInsert jdbcInsert(String tableName) {
		return new SimpleJdbcInsert(containerdbJdbcTemplate.getDataSource()).withTableName(tableName);
	}

	public java.sql.Array createSqlArray(Object[] array) {
		String type = null;
		Object v = array[0];
		if (v instanceof Boolean) {
			type = "BIT";
		} else if (v instanceof Integer) {
			type = "INT";
		} else if (v instanceof Long) {
			type = "BIGINT";
		} else if (v instanceof Double) {
			type = "DOUBLE";
		} else if (v instanceof String) {
			type = "TEXT";
		}else if (v instanceof Character) {
			type = "CHAR";
		} else if (v instanceof Timestamp) {
			type = "TIMESTAMP";
		} else if (v instanceof Date || v instanceof java.sql.Date) {
			type = "DATE";
		}
		Array dSqlArray;
		try {
			dSqlArray = containerdbJdbcTemplate.getDataSource().getConnection().createArrayOf(type, array);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return dSqlArray;
	}
	public boolean tableExists(String tableName) {
		
		DatabaseMetaData dbm;
		Boolean exists = false;
		try {
			dbm = containerdbJdbcTemplate.getDataSource().getConnection().getMetaData();
			ResultSet rs = dbm.getTables(null, null, tableName, null);
			if (rs.next()) {
				exists = true;
			} 
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return exists;
	}
	
	public void runFeatureFunction(String functionQuery) {
		containerdbJdbcTemplate.execute(functionQuery);
	}
	/**
	 * 
	 * @param queryStr
	 * @param tranKey
	 * @return
	 */
	public List<Map<String, Object>> runFeaturesQuery(String queryStr) {
		List<Map<String, Object>> restults = new ArrayList<Map<String, Object>>();
		log.debug("running the query for features -- "+queryStr +" ....");
		containerdbJdbcTemplate.query(queryStr, (ResultSet rs) -> {
			Map<String, Object> map = new HashMap<String, Object>();
			ResultSetMetaData rmd = rs.getMetaData();
			List<String> columns = new ArrayList<String>();
			for (int i = 1; i <= rmd.getColumnCount(); i++) {
				columns.add(rmd.getColumnLabel(i));
			}
			for (String k : columns) {
				if(rs.getObject(k) instanceof org.postgresql.jdbc.PgArray) {
					map.put(k, rs.getArray(k).getArray());
				}else {
					map.put(k, rs.getObject(k));
				}
			}
			restults.add(map);
		});
		return restults;
	}
	/**
	 * 
	 * @param tranKey
	 * @param tables
	 */
	public void clearRows(String tranKey, List<String> tables) {
		String[] sqls = new String[tables.size()];
		int c=0;
		for(String table: tables) {
			sqls[c] = "DELETE from " + table + " WHERE tranKey='"+tranKey+"'";
			c++;
		}
		containerdbJdbcTemplate.batchUpdate(sqls);
	}
	public void crateFunctions(String newSchema, Map<String, String> functionDefs) { 
		functionDefs.forEach((functionName, functionSource) -> {
			String function = functionSource.replace(appProps.getFeaturesSchema(), newSchema);
			containerdbJdbcTemplate.execute(function);
		});
	}
}
