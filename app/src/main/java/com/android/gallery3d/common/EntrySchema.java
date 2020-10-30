/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.common;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;

public final class EntrySchema {
    public static final int TYPE_STRING = 0;
    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_SHORT = 2;
    public static final int TYPE_INT = 3;
    public static final int TYPE_LONG = 4;
    public static final int TYPE_FLOAT = 5;
    public static final int TYPE_DOUBLE = 6;
    public static final int TYPE_BLOB = 7;
    @SuppressWarnings("unused")
    private static final String TAG = "EntrySchema";
    private static final String[] SQLITE_TYPES = {"TEXT", "INTEGER", "INTEGER", "INTEGER", "INTEGER", "REAL", "REAL", "NONE"};

    private static final String FULL_TEXT_INDEX_SUFFIX = "_fulltext";

    private final String mTableName;
    private final ColumnInfo[] mColumnInfo;
    private final String[] mProjection;
    private final boolean mHasFullTextIndex;

    public EntrySchema(Class<? extends Entry> clazz) {
        // Get table and column metadata from reflection.
        ColumnInfo[] columns = parseColumnInfo(clazz);
        mTableName = parseTableName(clazz);
        mColumnInfo = columns;

        // Cache the list of projection columns and check for full-text columns.
        String[] projection = {};
        boolean hasFullTextIndex = false;
        if (columns != null) {
            projection = new String[columns.length];
            for (int i = 0; i != columns.length; ++i) {
                ColumnInfo column = columns[i];
                projection[i] = column.name;
                if (column.fullText) {
                    hasFullTextIndex = true;
                }
            }
        }
        mProjection = projection;
        mHasFullTextIndex = hasFullTextIndex;
    }

    public String getTableName() {
        return mTableName;
    }

    public ColumnInfo[] getColumnInfo() {
        return mColumnInfo;
    }

    public String[] getProjection() {
        return mProjection;
    }

    public int getColumnIndex(String columnName) {
        for (ColumnInfo column : mColumnInfo) {
            if (column.name.equals(columnName)) {
                return column.projectionIndex;
            }
        }
        return -1;
    }

    public ColumnInfo getColumn(String columnName) {
        int index = getColumnIndex(columnName);
        return (index < 0) ? null : mColumnInfo[index];
    }

    private void logExecSql(SQLiteDatabase db, String sql) {
        db.execSQL(sql);
    }

    public <T extends Entry> T cursorToObject(Cursor cursor, T object) {
        try {
            for (ColumnInfo column : mColumnInfo) {
                int columnIndex = column.projectionIndex;
                Field field = column.field;
                switch (column.type) {
                    case TYPE_STRING:
                        field.set(object, cursor.isNull(columnIndex) ? null : cursor.getString(columnIndex));
                        break;
                    case TYPE_BOOLEAN:
                        field.setBoolean(object, cursor.getShort(columnIndex) == 1);
                        break;
                    case TYPE_SHORT:
                        field.setShort(object, cursor.getShort(columnIndex));
                        break;
                    case TYPE_INT:
                        field.setInt(object, cursor.getInt(columnIndex));
                        break;
                    case TYPE_LONG:
                        field.setLong(object, cursor.getLong(columnIndex));
                        break;
                    case TYPE_FLOAT:
                        field.setFloat(object, cursor.getFloat(columnIndex));
                        break;
                    case TYPE_DOUBLE:
                        field.setDouble(object, cursor.getDouble(columnIndex));
                        break;
                    case TYPE_BLOB:
                        field.set(object, cursor.isNull(columnIndex) ? null : cursor.getBlob(columnIndex));
                        break;
                }
            }
            return object;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void setIfNotNull(Field field, Object object, Object value) throws IllegalAccessException {
        if (value != null) field.set(object, value);
    }

    /**
     * Converts the ContentValues to the object. The ContentValues may not
     * contain values for all the fields in the object.
     */
    public <T extends Entry> T valuesToObject(ContentValues values, T object) {
        try {
            for (ColumnInfo column : mColumnInfo) {
                String columnName = column.name;
                Field field = column.field;
                switch (column.type) {
                    case TYPE_STRING:
                        setIfNotNull(field, object, values.getAsString(columnName));
                        break;
                    case TYPE_BOOLEAN:
                        setIfNotNull(field, object, values.getAsBoolean(columnName));
                        break;
                    case TYPE_SHORT:
                        setIfNotNull(field, object, values.getAsShort(columnName));
                        break;
                    case TYPE_INT:
                        setIfNotNull(field, object, values.getAsInteger(columnName));
                        break;
                    case TYPE_LONG:
                        setIfNotNull(field, object, values.getAsLong(columnName));
                        break;
                    case TYPE_FLOAT:
                        setIfNotNull(field, object, values.getAsFloat(columnName));
                        break;
                    case TYPE_DOUBLE:
                        setIfNotNull(field, object, values.getAsDouble(columnName));
                        break;
                    case TYPE_BLOB:
                        setIfNotNull(field, object, values.getAsByteArray(columnName));
                        break;
                }
            }
            return object;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void objectToValues(Entry object, ContentValues values) {
        try {
            for (ColumnInfo column : mColumnInfo) {
                String columnName = column.name;
                Field field = column.field;
                switch (column.type) {
                    case TYPE_STRING:
                        values.put(columnName, (String) field.get(object));
                        break;
                    case TYPE_BOOLEAN:
                        values.put(columnName, field.getBoolean(object));
                        break;
                    case TYPE_SHORT:
                        values.put(columnName, field.getShort(object));
                        break;
                    case TYPE_INT:
                        values.put(columnName, field.getInt(object));
                        break;
                    case TYPE_LONG:
                        values.put(columnName, field.getLong(object));
                        break;
                    case TYPE_FLOAT:
                        values.put(columnName, field.getFloat(object));
                        break;
                    case TYPE_DOUBLE:
                        values.put(columnName, field.getDouble(object));
                        break;
                    case TYPE_BLOB:
                        values.put(columnName, (byte[]) field.get(object));
                        break;
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public String toDebugString(Entry entry) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("ID=").append(entry.id);
            for (ColumnInfo column : mColumnInfo) {
                String columnName = column.name;
                Field field = column.field;
                Object value = field.get(entry);
                sb.append(" ").append(columnName).append("=").append((value == null) ? "null" : value.toString());
            }
            return sb.toString();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public String toDebugString(Entry entry, String... columnNames) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("ID=").append(entry.id);
            for (String columnName : columnNames) {
                ColumnInfo column = getColumn(columnName);
                Field field = column.field;
                Object value = field.get(entry);
                sb.append(" ").append(columnName).append("=").append((value == null) ? "null" : value.toString());
            }
            return sb.toString();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Cursor queryAll(SQLiteDatabase db) {
        return db.query(mTableName, mProjection, null, null, null, null, null);
    }

    public boolean queryWithId(SQLiteDatabase db, long id, Entry entry) {
        Cursor cursor = db.query(mTableName, mProjection, "_id=?", new String[]{Long.toString(id)}, null, null, null);
        boolean success = false;
        if (cursor.moveToFirst()) {
            cursorToObject(cursor, entry);
            success = true;
        }
        cursor.close();
        return success;
    }

    public long insertOrReplace(SQLiteDatabase db, Entry entry) {
        ContentValues values = new ContentValues();
        objectToValues(entry, values);
        if (entry.id == 0) {
            values.remove("_id");
        }
        long id = db.replace(mTableName, "_id", values);
        entry.id = id;
        return id;
    }

    public boolean deleteWithId(SQLiteDatabase db, long id) {
        return db.delete(mTableName, "_id=?", new String[]{Long.toString(id)}) == 1;
    }

    public void createTables(SQLiteDatabase db) {
        // Wrapped class must have a @Table.Definition.
        String tableName = mTableName;
        Utils.assertTrue(tableName != null);

        // Add the CREATE TABLE statement for the main table.
        StringBuilder sql = new StringBuilder("CREATE TABLE ");
        sql.append(tableName);
        sql.append(" (_id INTEGER PRIMARY KEY AUTOINCREMENT");
        for (ColumnInfo column : mColumnInfo) {
            if (!column.isId()) {
                sql.append(',');
                sql.append(column.name);
                sql.append(' ');
                sql.append(SQLITE_TYPES[column.type]);
                if (!TextUtils.isEmpty(column.defaultValue)) {
                    sql.append(" DEFAULT ");
                    sql.append(column.defaultValue);
                }
            }
        }
        sql.append(");");
        logExecSql(db, sql.toString());
        sql.setLength(0);

        // Create indexes for all indexed columns.
        for (ColumnInfo column : mColumnInfo) {
            // Create an index on the indexed columns.
            if (column.indexed) {
                sql.append("CREATE INDEX ");
                sql.append(tableName);
                sql.append("_index_");
                sql.append(column.name);
                sql.append(" ON ");
                sql.append(tableName);
                sql.append(" (");
                sql.append(column.name);
                sql.append(");");
                logExecSql(db, sql.toString());
                sql.setLength(0);
            }
        }

        if (mHasFullTextIndex) {
            // Add an FTS virtual table if using full-text search.
            String ftsTableName = tableName + FULL_TEXT_INDEX_SUFFIX;
            sql.append("CREATE VIRTUAL TABLE ");
            sql.append(ftsTableName);
            sql.append(" USING FTS3 (_id INTEGER PRIMARY KEY");
            for (ColumnInfo column : mColumnInfo) {
                if (column.fullText) {
                    // Add the column to the FTS table.
                    String columnName = column.name;
                    sql.append(',');
                    sql.append(columnName);
                    sql.append(" TEXT");
                }
            }
            sql.append(");");
            logExecSql(db, sql.toString());
            sql.setLength(0);

            // Build an insert statement that will automatically keep the FTS
            // table in sync.
            StringBuilder insertSql = new StringBuilder("INSERT OR REPLACE INTO ");
            insertSql.append(ftsTableName);
            insertSql.append(" (_id");
            for (ColumnInfo column : mColumnInfo) {
                if (column.fullText) {
                    insertSql.append(',');
                    insertSql.append(column.name);
                }
            }
            insertSql.append(") VALUES (new._id");
            for (ColumnInfo column : mColumnInfo) {
                if (column.fullText) {
                    insertSql.append(",new.");
                    insertSql.append(column.name);
                }
            }
            insertSql.append(");");
            String insertSqlString = insertSql.toString();

            // Add an insert trigger.
            sql.append("CREATE TRIGGER ");
            sql.append(tableName);
            sql.append("_insert_trigger AFTER INSERT ON ");
            sql.append(tableName);
            sql.append(" FOR EACH ROW BEGIN ");
            sql.append(insertSqlString);
            sql.append("END;");
            logExecSql(db, sql.toString());
            sql.setLength(0);

            // Add an update trigger.
            sql.append("CREATE TRIGGER ");
            sql.append(tableName);
            sql.append("_update_trigger AFTER UPDATE ON ");
            sql.append(tableName);
            sql.append(" FOR EACH ROW BEGIN ");
            sql.append(insertSqlString);
            sql.append("END;");
            logExecSql(db, sql.toString());
            sql.setLength(0);

            // Add a delete trigger.
            sql.append("CREATE TRIGGER ");
            sql.append(tableName);
            sql.append("_delete_trigger AFTER DELETE ON ");
            sql.append(tableName);
            sql.append(" FOR EACH ROW BEGIN DELETE FROM ");
            sql.append(ftsTableName);
            sql.append(" WHERE _id = old._id; END;");
            logExecSql(db, sql.toString());
            sql.setLength(0);
        }
    }

    public void dropTables(SQLiteDatabase db) {
        String tableName = mTableName;
        StringBuilder sql = new StringBuilder("DROP TABLE IF EXISTS ");
        sql.append(tableName);
        sql.append(';');
        logExecSql(db, sql.toString());
        sql.setLength(0);

        if (mHasFullTextIndex) {
            sql.append("DROP TABLE IF EXISTS ");
            sql.append(tableName);
            sql.append(FULL_TEXT_INDEX_SUFFIX);
            sql.append(';');
            logExecSql(db, sql.toString());
        }

    }

    public void deleteAll(SQLiteDatabase db) {
        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(mTableName);
        sql.append(";");
        logExecSql(db, sql.toString());
    }

    private String parseTableName(Class<? extends Object> clazz) {
        // Check for a table annotation.
        Entry.Table table = clazz.getAnnotation(Entry.Table.class);
        if (table == null) {
            return null;
        }

        // Return the table name.
        return table.value();
    }

    private ColumnInfo[] parseColumnInfo(Class<? extends Object> clazz) {
        ArrayList<ColumnInfo> columns = new ArrayList<ColumnInfo>();
        while (clazz != null) {
            parseColumnInfo(clazz, columns);
            clazz = clazz.getSuperclass();
        }

        // Return a list.
        ColumnInfo[] columnList = new ColumnInfo[columns.size()];
        columns.toArray(columnList);
        return columnList;
    }

    private void parseColumnInfo(Class<? extends Object> clazz, ArrayList<ColumnInfo> columns) {
        // Gather metadata from each annotated field.
        Field[] fields = clazz.getDeclaredFields(); // including non-public fields
        for (int i = 0; i != fields.length; ++i) {
            // Get column metadata from the annotation.
            Field field = fields[i];
            Entry.Column info = ((AnnotatedElement) field).getAnnotation(Entry.Column.class);
            if (info == null) continue;

            // Determine the field type.
            int type;
            Class<?> fieldType = field.getType();
            if (fieldType == String.class) {
                type = TYPE_STRING;
            } else if (fieldType == boolean.class) {
                type = TYPE_BOOLEAN;
            } else if (fieldType == short.class) {
                type = TYPE_SHORT;
            } else if (fieldType == int.class) {
                type = TYPE_INT;
            } else if (fieldType == long.class) {
                type = TYPE_LONG;
            } else if (fieldType == float.class) {
                type = TYPE_FLOAT;
            } else if (fieldType == double.class) {
                type = TYPE_DOUBLE;
            } else if (fieldType == byte[].class) {
                type = TYPE_BLOB;
            } else {
                throw new IllegalArgumentException("Unsupported field type for column: " + fieldType.getName());
            }

            // Add the column to the array.
            int index = columns.size();
            columns.add(new ColumnInfo(info.value(), type, info.indexed(), info.fullText(), info.defaultValue(), field, index));
        }
    }

    public static final class ColumnInfo {
        private static final String ID_KEY = "_id";

        public final String name;
        public final int type;
        public final boolean indexed;
        public final boolean fullText;
        public final String defaultValue;
        public final Field field;
        public final int projectionIndex;

        public ColumnInfo(String name, int type, boolean indexed, boolean fullText, String defaultValue, Field field, int projectionIndex) {
            this.name = name.toLowerCase();
            this.type = type;
            this.indexed = indexed;
            this.fullText = fullText;
            this.defaultValue = defaultValue;
            this.field = field;
            this.projectionIndex = projectionIndex;

            field.setAccessible(true); // in order to set non-public fields
        }

        public boolean isId() {
            return ID_KEY.equals(name);
        }
    }
}
