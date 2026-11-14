package com.fongmi.android.tv.db.dao;

import androidx.annotation.NonNull;
import androidx.room.EntityDeleteOrUpdateAdapter;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import com.fongmi.android.tv.bean.Site;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;
import kotlin.Unit;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class SiteDao_Impl extends SiteDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<Site> __insertAdapterOfSite;

  private final EntityDeleteOrUpdateAdapter<Site> __updateAdapterOfSite;

  private final EntityDeleteOrUpdateAdapter<Site> __updateAdapterOfSite_1;

  public SiteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfSite = new EntityInsertAdapter<Site>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `Site` (`key`,`searchable`,`changeable`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Site entity) {
        if (entity.getKey() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getKey());
        }
        if (entity.getSearchable() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getSearchable());
        }
        if (entity.getChangeable() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getChangeable());
        }
      }
    };
    this.__updateAdapterOfSite = new EntityDeleteOrUpdateAdapter<Site>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR REPLACE `Site` SET `key` = ?,`searchable` = ?,`changeable` = ? WHERE `key` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Site entity) {
        if (entity.getKey() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getKey());
        }
        if (entity.getSearchable() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getSearchable());
        }
        if (entity.getChangeable() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getChangeable());
        }
        if (entity.getKey() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getKey());
        }
      }
    };
    this.__updateAdapterOfSite_1 = new EntityDeleteOrUpdateAdapter<Site>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `Site` SET `key` = ?,`searchable` = ?,`changeable` = ? WHERE `key` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Site entity) {
        if (entity.getKey() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getKey());
        }
        if (entity.getSearchable() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getSearchable());
        }
        if (entity.getChangeable() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getChangeable());
        }
        if (entity.getKey() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getKey());
        }
      }
    };
  }

  @Override
  public Long insert(final Site item) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfSite.insertAndReturnId(_connection, item);
    });
  }

  @Override
  public List<Long> insert(final List<Site> items) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfSite.insertAndReturnIdsList(_connection, items);
    });
  }

  @Override
  public void update(final Site item) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfSite.handle(_connection, item);
      return null;
    });
  }

  @Override
  public void update(final List<Site> items) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfSite_1.handleMultiple(_connection, items);
      return null;
    });
  }

  @Override
  public void insertOrUpdate(final Site item) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      SiteDao_Impl.super.insertOrUpdate(item);
      return Unit.INSTANCE;
    });
  }

  @Override
  public void insertOrUpdate(final List<Site> items) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      SiteDao_Impl.super.insertOrUpdate(items);
      return Unit.INSTANCE;
    });
  }

  @Override
  public List<Site> findAll() {
    final String _sql = "SELECT * FROM Site";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "key");
        final int _columnIndexOfSearchable = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "searchable");
        final int _columnIndexOfChangeable = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "changeable");
        final List<Site> _result = new ArrayList<Site>();
        while (_stmt.step()) {
          final Site _item;
          _item = new Site();
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfKey)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfKey);
          }
          _item.setKey(_tmpKey);
          final Integer _tmpSearchable;
          if (_stmt.isNull(_columnIndexOfSearchable)) {
            _tmpSearchable = null;
          } else {
            _tmpSearchable = (int) (_stmt.getLong(_columnIndexOfSearchable));
          }
          _item.setSearchable(_tmpSearchable);
          final Integer _tmpChangeable;
          if (_stmt.isNull(_columnIndexOfChangeable)) {
            _tmpChangeable = null;
          } else {
            _tmpChangeable = (int) (_stmt.getLong(_columnIndexOfChangeable));
          }
          _item.setChangeable(_tmpChangeable);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void delete() {
    final String _sql = "DELETE FROM Site";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
