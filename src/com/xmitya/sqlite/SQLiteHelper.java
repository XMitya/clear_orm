package com.xmitya.sqlite;

import java.lang.reflect.Field;

import com.xmitya.sqlite.orm.Dao;
import com.xmitya.sqlite.orm.SQLiteField;
import com.xmitya.sqlite.orm.SQLiteTable;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLiteHelper extends SQLiteOpenHelper {

	 private static final String DATABASE_NAME = "database.db";
	 private static final int DATABASE_VERSION = 1;
	
	 /**
	  * Entities classes which will be processed with {@link Dao}
	  */
	private static Class<?>[] entities = {};
	
	/**
	 * Return internal entities array which enumerates tables for using in {@link Dao} 
	 * @return
	 */
	public static Class<?>[] getEntities() {
		return entities;
	}

	/** 
	 * Set internal entities array which enumerates tables for using in {@link Dao}. 
	 * @param entities
	 */
	public static void setEntities(Class<?>[] entities) {
		SQLiteHelper.entities = entities;
	}

	public SQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		createTablesIfNotExists(database);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		if(newVersion > oldVersion){
			dropTables(database);
			createTablesIfNotExists(database);
		}
	}

	/**
	 * Delete all tables enumerated in internal <b>entities</b> array.
	 * @param database
	 */
	private void dropTables(SQLiteDatabase database){
		for(Class<?> clazz : entities){
			StringBuilder builder = new StringBuilder("DROP TABLE ");
			SQLiteTable tableNameAn = clazz.getAnnotation(SQLiteTable.class);
			// skip if table not annotated
			if(tableNameAn == null) continue;
			builder.append(tableNameAn.tableName()).append(";");
			Log.i("<<ARNIKA>>", builder.toString());
			database.execSQL(builder.toString());
		}
	}
	
	/**
	 * Creates tables enumerated in internal <b>entities</b> array if they not exists in database. 
	 * @param database
	 */
	private void createTablesIfNotExists(SQLiteDatabase database){
		for(Class<?> clazz : entities){
			StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
			SQLiteTable tableNameAn = clazz.getAnnotation(SQLiteTable.class);
			// skip if table not annotated
			if(tableNameAn == null) continue;
			builder.append(tableNameAn.tableName()).append(" (");
			// search fields and column names
			Field[] fields = clazz.getDeclaredFields();
			for(Field field : fields){
				SQLiteField fieldAn = field.getAnnotation(SQLiteField.class);
				// skip if field not annotated
				if(fieldAn == null) continue;
				builder.append(" ").append(fieldAn.columnName());
				if(fieldAn.id()){
					builder.append(" PRIMARY KEY");
					if(fieldAn.autoGenerate()){
						builder.append(" AUTOINCREMENT");
					}
				}
				builder.append(",");
			}
			//remove last comma
			builder.deleteCharAt(builder.length() - 1);
			builder.append(");");
			Log.i("<<ARNIKA>>", builder.toString());
			database.execSQL(builder.toString());
		}
	}
}
