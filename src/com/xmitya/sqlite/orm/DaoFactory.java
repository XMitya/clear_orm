package com.xmitya.sqlite.orm;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.xmitya.sqlite.SQLiteHelper;
/**
 * DaoFactory allows you to cache all created {@link Dao}, and only one method for create this Dao:
 * <p><pre>
 * Dao<Entity> dao = DaoFactory.getInstance(context).getWriterDao(Entity.class);
 * </pre>
 * 
 * DaoFactory constructor creates SQLiteHelper instance and invokes {@link SQLiteHelper#onCreate(android.database.sqlite.SQLiteDatabase)} method.
 * @author xmitya
 *
 */
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
	
	/**
	 * Creates if not exists and returns DaoFactory instance. 
	 * @param context
	 * @return
	 */
	public synchronized static DaoFactory getInstance(Context context){
		if(instance == null){
			instance = new DaoFactory(context);
		}
		return instance;
	}
	
	public SQLiteHelper getHelper(){
		return helper;
	}
	
	public void clearDatabase(){
		helper.onUpgrade(helper.getWritableDatabase(), 1, 2);
	}
	
	/**
	 * Returns cached or creates new instance of readable {@link Dao}
	 * @param clazz
	 * @return
	 */
	public synchronized <T> Dao<T> getReaderDao(Class<T> clazz){
		@SuppressWarnings("unchecked")
		Dao<T> dao = (Dao<T>) readerDatabases.get(clazz);
		if(dao == null || !dao.isOpen()){
			dao = new Dao<T>(helper.getReadableDatabase(), clazz);
			readerDatabases.put(clazz, dao);
		}
		return dao;
	}
	
	/**
	 * Returns or creates new instance of writable {@link Dao}
	 * @param clazz
	 * @return
	 */
	public synchronized <T> Dao<T> getWriterDao(Class<T> clazz){
		@SuppressWarnings("unchecked")
		Dao<T> dao = (Dao<T>) writerDatabases.get(clazz);
		if(dao == null || !dao.isOpen()){
			dao = new Dao<T>(helper.getWritableDatabase(), clazz);
			writerDatabases.put(clazz, dao);
		}
		return dao;
	}
	
	/**
	 * Close all open database connections and destroy {@link DaoFactory} instance.
	 */
	public synchronized static void close(){
		if(instance != null){
			instance.helper.close();
			instance = null;
		}
	}
}
