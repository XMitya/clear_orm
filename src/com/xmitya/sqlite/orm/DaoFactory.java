package com.xmitya.sqlite.orm;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.xmitya.sqlite.SQLiteHelper;

public class DaoFactory {

	private DaoFactory(Context context) {
		helper = new SQLiteHelper(context);
		helper.onCreate(helper.getWritableDatabase());
//		helper.onUpgrade(helper.getWritableDatabase(), 1, 2);
		readerDatabases = new HashMap<Class<?>, Dao<?>>();
		writerDatabases = new HashMap<Class<?>, Dao<?>>();
	}
	
	private SQLiteHelper helper;
	private Map<Class<?>, Dao<?>> readerDatabases;
	private Map<Class<?>, Dao<?>> writerDatabases;
	private static DaoFactory instance;
	
	public synchronized static DaoFactory getInstance(Context context){
		if(instance == null){
			instance = new DaoFactory(context);
		}
		return instance;
	}
	
	public synchronized <T> Dao<T> getReaderDao(Class<T> clazz){
		@SuppressWarnings("unchecked")
		Dao<T> dao = (Dao<T>) readerDatabases.get(clazz);
		if(dao == null || !dao.isOpen()){
			dao = new Dao<T>(helper.getReadableDatabase(), clazz);
			readerDatabases.put(clazz, dao);
		}
		return dao;
	}
	
	public synchronized <T> Dao<T> getWriterDao(Class<T> clazz){
		@SuppressWarnings("unchecked")
		Dao<T> dao = (Dao<T>) writerDatabases.get(clazz);
		if(dao == null || !dao.isOpen()){
			dao = new Dao<T>(helper.getWritableDatabase(), clazz);
			writerDatabases.put(clazz, dao);
		}
		return dao;
	}
	
	public synchronized static void close(){
		if(instance != null){
			instance.helper.close();
			instance = null;
		}
	}
}
