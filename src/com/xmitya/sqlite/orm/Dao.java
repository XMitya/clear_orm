package com.xmitya.sqlite.orm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.xmitya.sqlite.SQLiteHelper;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Lite DAO for Android SQLite. All tables annotated with {@link SQLiteTable}
 * should be created in {@link SQLiteHelper}.
 * 
 * @author xmitya
 * 
 * @param <T>
 */
public class Dao<T> {

	private SQLiteDatabase database;
	private String tableName;
	private Class<T> clazz;
	private String idField;

	/**
	 * Creates new DAO instance. Before using table should be created.
	 * 
	 * @param database
	 * @param clazz
	 *            Type for objects which will be processed.
	 * @throws IllegalArgumentException
	 *             if table not annotated
	 */
	public Dao(SQLiteDatabase database, Class<T> clazz) {
		this.database = database;
		this.clazz = clazz;
		SQLiteTable tableNameAn = clazz.getAnnotation(SQLiteTable.class);
		if (tableNameAn == null) {
			throw new IllegalArgumentException(String.format(
					"Class %s not annotated with SQLiteTable annotation",
					clazz.getName()));
		}
		tableName = tableNameAn.tableName();
		findIdFieldName();
	}

	private void findIdFieldName() {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			SQLiteField fieldAn = field.getAnnotation(SQLiteField.class);
			if (fieldAn == null)
				continue;
			if (fieldAn.id()) {
				idField = fieldAn.columnName();
				return;
			}
		}
		throw new IllegalStateException(
				"No ID field found. One field should be marked as id.");
	}

	/**
	 * Close database connection.
	 */
	public void close() {
		database.close();
	}

	/**
	 * Shows if database connection open.
	 * 
	 * @return
	 */
	public boolean isOpen() {
		return database.isOpen();
	}

	/**
	 * Drop table with all data.
	 */
	public void dropTable() {
		SQLiteHelper.dropTable(database, clazz);
	}

	/**
	 * Create new table if it's not exists.
	 */
	public void createTableIfNotExists() {
		SQLiteHelper.createTableIfNotExists(database, clazz);
	}

	/**
	 * Insert object into table. If field is null it will be skipped.
	 * 
	 * @param data
	 * @throws SQLException
	 */
	public void insert(T data) throws SQLException {
		StringBuilder builder = new StringBuilder("INSERT INTO ").append(
				tableName).append(" (");
		StringBuilder values = new StringBuilder(" VALUES(");
		Field[] fields = clazz.getDeclaredFields();
		try {
			for (Field field : fields) {
				SQLiteField fieldAn = field.getAnnotation(SQLiteField.class);
				if (fieldAn == null)
					continue;
				// get value
				Object value = getValue(data, field);
				if (value != null) {
					// check if exists date pattern for formatting date to string
					if(!"".equals(fieldAn.datePattern()) && fieldAn.datePattern() != null){
						SimpleDateFormat format = new SimpleDateFormat(fieldAn.datePattern());
						value = format.format((Date)value);
					}
					values.append("'").append(value.toString()).append("'")
							.append(',');
					builder.append(fieldAn.columnName()).append(',');
				}
			}
			builder.deleteCharAt(builder.length() - 1).append(")");
			values.deleteCharAt(values.length() - 1);
			values.append(");");
			builder.append(values.toString());
		} catch (Exception e) {
			throw new SQLException("Error on insert " + e.getMessage());
		}
		database.execSQL(builder.toString());
	}

	private Object getValue(T data, Field field) throws SecurityException,
			NoSuchMethodException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		String getterName = field.getName();
		char letter = getterName.charAt(0);
		letter = Character.toUpperCase(letter);
		getterName = getterName.substring(1);
		getterName = "get" + letter + getterName;
		Method method = clazz.getMethod(getterName);
		Object result = method.invoke(data);
		return result;
	}

	/**
	 * SQL UPDATE command. If field is null empty string will be inserted
	 * instead.
	 * 
	 * @param data
	 * @throws SQLException
	 */
	public void update(T data) throws SQLException {
		StringBuilder builder = new StringBuilder("UPDATE ").append(tableName)
				.append(" SET ");
		Field[] fields = clazz.getDeclaredFields();
		Object idValue = null;
		try {
			for (Field field : fields) {
				SQLiteField fieldAn = field.getAnnotation(SQLiteField.class);
				if (fieldAn == null)
					continue;
				// get value
				Object value = getValue(data, field);
				// save id for WHERE statement
				if (fieldAn.id()) {
					idValue = value;
				}
				if (value == null) {
					value = "";
				}
				builder.append(fieldAn.columnName()).append("='")
						.append(value.toString()).append("',");

			}
			builder.deleteCharAt(builder.length() - 1);
			builder.append(" WHERE ").append(idField).append("='")
					.append(idValue).append("';");
		} catch (Exception e) {
			throw new SQLException("Error on update " + e.getMessage());
		}
		database.execSQL(builder.toString());
	}

	/**
	 * Get object by it's id. If no entries found null will be returned.
	 * 
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public T selectById(Object id) throws SQLException {
		StringBuilder builder = new StringBuilder("SELECT * FROM ")
				.append(tableName).append(" WHERE ").append(idField)
				.append("=").append("'").append(id.toString()).append("'");
		Cursor cursor = null;
		T data = null;
		try {
			cursor = database.rawQuery(builder.toString(), new String[] {});
			cursor.moveToFirst();
			if (cursor.isAfterLast()) {
				return null;
			}
			data = cursorToObject(cursor);
		} catch (Exception e) {
			e.printStackTrace();
			throw new SQLException("Error on select by ID " + e.getMessage());
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return data;
	}

	private T cursorToObject(Cursor cursor) throws SecurityException,
			NoSuchMethodException, IllegalArgumentException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, ParseException {
		Field[] fields = clazz.getDeclaredFields();
		Constructor<T> constructor = clazz.getConstructor();
		T data = constructor.newInstance();
		for (int i = 0, col = 0, l = fields.length; i < l; i++) {
			SQLiteField fieldAn = fields[i].getAnnotation(SQLiteField.class);
			if (fieldAn == null) {
				continue;
			}
			Class<?> type = fields[i].getType();
			if (type == Integer.class) {
				Integer value = cursor.getInt(col);
				setField(data, fields[i], type, value);
			} else if (type == int.class) {
				int value = cursor.getInt(col);
				setField(data, fields[i], type, value);
			} else if (type == Long.class) {
				Long value = cursor.getLong(col);
				setField(data, fields[i], type, value);
			} else if (type == long.class) {
				long value = cursor.getLong(col);
				setField(data, fields[i], type, value);
			} else if (type == String.class) {
				String value = cursor.getString(col);
				setField(data, fields[i], type, value);
			} else if (type == Date.class) {
				if (!"".equals(fieldAn.datePattern()) && fieldAn.datePattern() != null) {
					SimpleDateFormat format = new SimpleDateFormat(
							fieldAn.datePattern());
					String dateStr = cursor.getString(col);
					if (dateStr != null) {
						Date value = format.parse(dateStr);
						setField(data, fields[i], type, value);
					}
				}
			} else if (type == byte[].class) {
				byte[] value = cursor.getBlob(col);
				setField(data, fields[i], type, value);
			}
			col++;
		}
		return data;
	}

	private Method getSetter(T data, Field field, Class<?> parameterType)
			throws SecurityException, NoSuchMethodException {
		String setterName = field.getName();
		char letter = setterName.charAt(0);
		letter = Character.toUpperCase(letter);
		setterName = setterName.substring(1);
		setterName = "set" + letter + setterName;
		Method method = clazz.getMethod(setterName, parameterType);
		return method;
	}


	private void setField(T data, Field field, Class<?> clazz, Object value)
			throws SecurityException, NoSuchMethodException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		Method method = getSetter(data, field, clazz);
		method.invoke(data, value);
	}

	/**
	 * Delete entry by id. Extracts id from id field and invokes
	 * {@link #deleteById(Object)}
	 * 
	 * @param data
	 * @throws SQLException
	 */
	public void delete(T data) throws SQLException {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			SQLiteField fieldAn = field.getAnnotation(SQLiteField.class);
			if (fieldAn == null)
				continue;
			if (fieldAn.id()) {
				try {
					Object idValue = getValue(data, field);
					deleteById(idValue);
				} catch (Exception e) {
					throw new SQLException("Error on deleting entry "
							+ e.getMessage());
				}
			}
		}
	}

	/**
	 * Delete entry by it's id.
	 * 
	 * @param id
	 */
	public void deleteById(Object id) {
		StringBuilder builder = new StringBuilder("DELETE FROM ")
				.append(tableName).append(" WHERE ").append(idField)
				.append(" ='").append(id.toString()).append("';");
		database.execSQL(builder.toString());
	}

	/**
	 * Get all values from table.
	 * 
	 * @return
	 * @throws SQLException
	 */
	public List<T> getAll() throws SQLException {
		StringBuilder builder = new StringBuilder("SELECT * FROM ")
				.append(tableName);
		Cursor cursor = null;
		List<T> result = new ArrayList<T>();
		T data = null;
		try {
			cursor = database.rawQuery(builder.toString(), new String[] {});
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				data = cursorToObject(cursor);
				result.add(data);
				cursor.moveToNext();
			}
		} catch (Exception e) {
			throw new SQLException("Error on select by ID " + e.getMessage());
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	private Object getIdValue(T data) throws SecurityException,
			IllegalArgumentException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			SQLiteField fieldAn = field.getAnnotation(SQLiteField.class);
			if (fieldAn == null)
				continue;
			if (fieldAn.id()) {
				Object value = getValue(data, field);
				return value;
			}
		}
		return null;
	}

	/**
	 * Try to find if row with such id exists, if yes try to update, if no -
	 * insert value.
	 * 
	 * @param data
	 * @throws SQLException
	 */
	public void insertOrUpdate(T data) throws SQLException {
		Cursor cursor = null;
		try {
			Object id = getIdValue(data);
			StringBuilder builder = new StringBuilder("SELECT * FROM ")
					.append(tableName).append(" WHERE ").append(idField)
					.append("=?");
			cursor = database.rawQuery(builder.toString(),
					new String[] { id.toString() });
			cursor.moveToFirst();
			boolean insert = cursor.isAfterLast();
			if (insert) {
				insert(data);
			} else {
				update(data);
			}
		} catch (Exception e) {
			throw new SQLException("Error on insert or update "
					+ e.getMessage());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
}
