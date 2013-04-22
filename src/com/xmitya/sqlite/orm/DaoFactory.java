package com.xmitya.sqlite.orm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.xmitya.sqlite.SQLiteHelper;

/**
 * DaoFactory allows you to cache all created {@link com.xmitya.sqlite.orm.Dao}, and only one method for create this Dao:
 * <p><pre>
 * Dao<Entity> dao = DaoFactory.getInstance(context).getWriterDao(Entity.class);
 * </pre>
 * <p/>
 * DaoFactory constructor creates SQLiteHelper instance and invokes
 * {@link com.xmitya.sqlite.SQLiteHelper#onCreate(android.database.sqlite.SQLiteDatabase)} method.
 *
 * @author xmitya
 */
public class DaoFactory {

    private File dbFile;
    private File localDbFile;
    public static final String DATA_FOLDER_PATH_PREFIX = "/data/data/";
    public static final String DATA_FOLDER_PATH_SUFFIX = "/databases/";

    private DaoFactory(Context context) {
        helper = new SQLiteHelper(context);
        helper.onCreate(helper.getWritableDatabase());
        helper.upgradeIfNeed(helper.getWritableDatabase());
        createMaps();
    }

    private void createMaps() {
        readerDatabases = new HashMap<Class<?>, Dao<?>>();
        writerDatabases = new HashMap<Class<?>, Dao<?>>();
    }

    private DaoFactory(Context context, File dbFile, int databaseVersion) throws IOException {
        this.dbFile = dbFile;
        // TODO upgrade if need
        String fileName = dbFile.getName();
        String packageName = context.getPackageName();
        String newFilePath = DATA_FOLDER_PATH_PREFIX + packageName
                + DATA_FOLDER_PATH_SUFFIX + fileName;
        File localFile = new File(newFilePath);
        for (int i = 0; localFile.exists(); i++) {
            localFile = new File(newFilePath + '.' + i);
        }
        localDbFile = localFile;
        if (dbFile.exists()) {
            copyFile(dbFile, localDbFile);
        }
        createMaps();
    }

    private SQLiteHelper helper;
    private Map<Class<?>, Dao<?>> readerDatabases;
    private Map<Class<?>, Dao<?>> writerDatabases;
    private static DaoFactory instance;

    private static HashMap<String, DaoFactory> customInstances;

    /**
     * Creates if not exists and returns DaoFactory instance.
     *
     * @param context
     * @return
     */
    public synchronized static DaoFactory getInstance(Context context) {
        if (instance == null) {
            instance = new DaoFactory(context);
        }
        return instance;
    }

    /**
     * Creates if not exists and returns DaoFactory instance for SQLite files
     * not in application environment. For accessing such files this file
     * copy to /data/data/APPLICATION_PACKAGE/databases/directory, changes and on
     * {@link #closeDatabase()} operation copies back and deletes from application
     * databases directory.
     * @param context for getting application package.
     * @param dbFile custom SQLite file.
     * @param databaseVersion version of this database if this file doesn't exists.
     * @return {@link com.xmitya.sqlite.orm.DaoFactory} instance.
     * @throws java.io.IOException
     */
    public synchronized static DaoFactory getCustomInstance(Context context,
                                                            File dbFile,
                                                            int databaseVersion)
            throws IOException {
        if (customInstances == null) {
            customInstances = new HashMap<String, DaoFactory>();
        }
        DaoFactory factory = customInstances.get(dbFile.getAbsolutePath());
        if (factory == null) {
            factory = new DaoFactory(context, dbFile, databaseVersion);
            customInstances.put(dbFile.getAbsolutePath(), factory);
        }

        return factory;
    }

    public SQLiteHelper getHelper() {
        return helper;
    }

    public void clearDatabase() {
        helper.onUpgrade(helper.getWritableDatabase(), 1, 2);
    }

    /**
     * Returns cached or creates new instance of readable {@link com.xmitya.sqlite.orm.Dao}
     *
     * @param clazz
     * @return
     */
    public synchronized <T> Dao<T> getReaderDao(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        Dao<T> dao = (Dao<T>) readerDatabases.get(clazz);
        if (dao == null || !dao.isOpen()) {
            SQLiteDatabase database;
            if (helper != null) {
                database = helper.getWritableDatabase();
            } else {
                database = SQLiteDatabase.openDatabase(localDbFile.getAbsolutePath(),
                        null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.CREATE_IF_NECESSARY);
            }
            SQLiteHelper.createTableIfNotExists(database, clazz);
            database.close();
            dao = new Dao<T>(helper.getReadableDatabase(), clazz);
            readerDatabases.put(clazz, dao);
        }
        return dao;
    }

    /**
     * Returns or creates new instance of writable {@link com.xmitya.sqlite.orm.Dao}
     *
     * @param clazz
     * @return
     */
    public synchronized <T> Dao<T> getWriterDao(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        Dao<T> dao = (Dao<T>) writerDatabases.get(clazz);
        if (dao == null || !dao.isOpen()) {
            SQLiteDatabase database;
            if (helper != null) {
                database = helper.getWritableDatabase();
            } else {
                database = SQLiteDatabase.openOrCreateDatabase(localDbFile, null);
            }
            dao = new Dao<T>(database, clazz);
            SQLiteHelper.createTableIfNotExists(database, clazz);
            writerDatabases.put(clazz, dao);
        }
        return dao;
    }

    /**
     * Close all open database connections and destroy {@link com.xmitya.sqlite.orm.DaoFactory} instance.
     */
    public synchronized static void close() throws IOException {
        if (instance != null) {
            instance.helper.close();
            instance = null;
            if (customInstances != null) {
                for (DaoFactory customFactory : customInstances.values()) {
                    customFactory.closeDatabase();
                }
                customInstances = null;
            }
        }
    }

    /**
     * Close all open database connections assigned to this factory. If it is
     * a custom DaoFactory SQLite file will be copied back and removed from
     * application database directory.
     * @throws java.io.IOException
     */
    public void closeDatabase() throws IOException {
        if (helper != null) {
            helper.close();
        }
        for (Dao dao : writerDatabases.values()) {
            dao.close();
        }
        for (Dao dao : readerDatabases.values()) {
            dao.close();
        }
        if (localDbFile.exists()) {
            copyFile(localDbFile, dbFile);
            localDbFile.delete();
            File journalFile = new File(localDbFile.getAbsolutePath() + "-journal");
            if (journalFile.exists()) {
                journalFile.delete();
            }
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        FileInputStream fin = null;
        FileOutputStream fout = null;
        try {
            fin = new FileInputStream(src);
            fout = new FileOutputStream(dst);
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = fin.read(buf)) > 0) {
                fout.write(buf, 0, len);
            }
        } finally {
            if (fin != null) {
                fin.close();
            }
            if (fout != null) {
                fout.close();
            }
        }
    }
}
