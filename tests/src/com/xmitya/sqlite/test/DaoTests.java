package com.xmitya.sqlite.test;

import java.sql.SQLException;
import java.util.Date;

import com.xmitya.sqlite.SQLiteHelper;
import com.xmitya.sqlite.orm.*;

import android.test.AndroidTestCase;

public class DaoTests extends AndroidTestCase{

	@Override
	protected void setUp() throws Exception {
		SQLiteHelper.setDatabaseName("test_database.db");
		SQLiteHelper.setEntities(new Class<?>[]{TestEntity.class});
	}
	
	@Override
	protected void tearDown() throws Exception {
		DaoFactory.close();
	}
	
	@SQLiteTable(tableName = "test_table")
	static class TestEntity{
		@SQLiteField(columnName = "_id", id = true)
		private String id;
		@SQLiteField(columnName = "number")
		private int number;
		@SQLiteField(columnName = "date", datePattern="yyyy-MM-dd HH:mm:ss:SSS")
		private Date date;
		public TestEntity() {
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public int getNumber() {
			return number;
		}
		public void setNumber(int number) {
			this.number = number;
		}
		public Date getDate() {
			return date;
		}
		public void setDate(Date date) {
			this.date = date;
		}
		
		@Override
		public boolean equals(Object o) {
			if(o == null) return false;
			if(this == o) return true;
			if(!(o instanceof TestEntity)) return false;
			TestEntity test = (TestEntity) o;
			if(id != null){
				if(!id.equals(test.id))	return false;
			}else{
				if(test.id != null) return false;
			}
			if(date != null){
				if(!date.equals(test.date))	return false;
			}else{
				if(test.date != null) return false;
			}
			if(number != test.number) return false;
			return true;
		}
	}
	
	public void testDao() throws SQLException{
		DaoFactory.getInstance(getContext()).clearDatabase();
		
		TestEntity test = new TestEntity();
		test.setDate(new Date());
		test.setId("my_entity");
		test.setNumber(10);
		
		Dao<TestEntity> testDao = DaoFactory.getInstance(getContext()).getWriterDao(TestEntity.class);
		testDao.insert(test);
		
		TestEntity test2 = testDao.selectById(test.getId());
		
		assertEquals(test, test2);
	}
}
