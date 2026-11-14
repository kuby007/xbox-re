package com.fongmi.android.tv.db.dao;

import androidx.annotation.NonNull;
import androidx.room.EntityDeleteOrUpdateAdapter;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import com.fongmi.android.tv.bean.Config;
import java.lang.Class;
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
public final class ConfigDao_Impl extends ConfigDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<Config> __insertAdapterOfConfig;

  private final EntityDeleteOrUpdateAdapter<Config> __updateAdapterOfConfig;

  private final EntityDeleteOrUpdateAdapter<Config> __updateAdapterOfConfig_1;

  public ConfigDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfConfig = new EntityInsertAdapter<Config>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `Config` (`id`,`type`,`time`,`url`,`json`,`name`,`logo`,`home`,`parse`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Config entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getType());
        statement.bindLong(3, entity.getTime());
        if (entity.getUrl() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getUrl());
        }
        if (entity.getJson() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getJson());
        }
        if (entity.getName() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getName());
        }
        if (entity.getLogo() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getLogo());
        }
        if (entity.getHome() == null) {
          statement.bindNull(8);
        } else {
          statement.bindText(8, entity.getHome());
        }
        if (entity.getParse() == null) {
          statement.bindNull(9);
        } else {
          statement.bindText(9, entity.getParse());
        }
      }
    };
    this.__updateAdapterOfConfig = new EntityDeleteOrUpdateAdapter<Config>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR REPLACE `Config` SET `id` = ?,`type` = ?,`time` = ?,`url` = ?,`json` = ?,`name` = ?,`logo` = ?,`home` = ?,`parse` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Config entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getType());
        statement.bindLong(3, entity.getTime());
        if (entity.getUrl() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getUrl());
        }
        if (entity.getJson() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getJson());
        }
        if (entity.getName() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getName());
        }
        if (entity.getLogo() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getLogo());
        }
        if (entity.getHome() == null) {
          statement.bindNull(8);
        } else {
          statement.bindText(8, entity.getHome());
        }
        if (entity.getParse() == null) {
          statement.bindNull(9);
        } else {
          statement.bindText(9, entity.getParse());
        }
        statement.bindLong(10, entity.getId());
      }
    };
    this.__updateAdapterOfConfig_1 = new EntityDeleteOrUpdateAdapter<Config>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `Config` SET `id` = ?,`type` = ?,`time` = ?,`url` = ?,`json` = ?,`name` = ?,`logo` = ?,`home` = ?,`parse` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Config entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getType());
        statement.bindLong(3, entity.getTime());
        if (entity.getUrl() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getUrl());
        }
        if (entity.getJson() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getJson());
        }
        if (entity.getName() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getName());
        }
        if (entity.getLogo() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getLogo());
        }
        if (entity.getHome() == null) {
          statement.bindNull(8);
        } else {
          statement.bindText(8, entity.getHome());
        }
        if (entity.getParse() == null) {
          statement.bindNull(9);
        } else {
          statement.bindText(9, entity.getParse());
        }
        statement.bindLong(10, entity.getId());
      }
    };
  }

  @Override
  public Long insert(final Config item) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfConfig.insertAndReturnId(_connection, item);
    });
  }

  @Override
  public List<Long> insert(final List<Config> items) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfConfig.insertAndReturnIdsList(_connection, items);
    });
  }

  @Override
  public void update(final Config item) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfConfig.handle(_connection, item);
      return null;
    });
  }

  @Override
  public void update(final List<Config> items) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfConfig_1.handleMultiple(_connection, items);
      return null;
    });
  }

  @Override
  public void insertOrUpdate(final Config item) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      ConfigDao_Impl.super.insertOrUpdate(item);
      return Unit.INSTANCE;
    });
  }

  @Override
  public void insertOrUpdate(final List<Config> items) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      ConfigDao_Impl.super.insertOrUpdate(items);
      return Unit.INSTANCE;
    });
  }

  @Override
  public List<Config> findAll() {
    final String _sql = "SELECT * FROM Config";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "type");
        final int _columnIndexOfTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "time");
        final int _columnIndexOfUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "url");
        final int _columnIndexOfJson = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "json");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfLogo = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "logo");
        final int _columnIndexOfHome = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "home");
        final int _columnIndexOfParse = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "parse");
        final List<Config> _result = new ArrayList<Config>();
        while (_stmt.step()) {
          final Config _item;
          _item = new Config();
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          _item.setId(_tmpId);
          final int _tmpType;
          _tmpType = (int) (_stmt.getLong(_columnIndexOfType));
          _item.setType(_tmpType);
          final long _tmpTime;
          _tmpTime = _stmt.getLong(_columnIndexOfTime);
          _item.setTime(_tmpTime);
          final String _tmpUrl;
          if (_stmt.isNull(_columnIndexOfUrl)) {
            _tmpUrl = null;
          } else {
            _tmpUrl = _stmt.getText(_columnIndexOfUrl);
          }
          _item.setUrl(_tmpUrl);
          final String _tmpJson;
          if (_stmt.isNull(_columnIndexOfJson)) {
            _tmpJson = null;
          } else {
            _tmpJson = _stmt.getText(_columnIndexOfJson);
          }
          _item.setJson(_tmpJson);
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          _item.setName(_tmpName);
          final String _tmpLogo;
          if (_stmt.isNull(_columnIndexOfLogo)) {
            _tmpLogo = null;
          } else {
            _tmpLogo = _stmt.getText(_columnIndexOfLogo);
          }
          _item.setLogo(_tmpLogo);
          final String _tmpHome;
          if (_stmt.isNull(_columnIndexOfHome)) {
            _tmpHome = null;
          } else {
            _tmpHome = _stmt.getText(_columnIndexOfHome);
          }
          _item.setHome(_tmpHome);
          final String _tmpParse;
          if (_stmt.isNull(_columnIndexOfParse)) {
            _tmpParse = null;
          } else {
            _tmpParse = _stmt.getText(_columnIndexOfParse);
          }
          _item.setParse(_tmpParse);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<Config> findByType(final int type) {
    final String _sql = "SELECT * FROM Config WHERE type = ? ORDER BY time DESC";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, type);
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "type");
        final int _columnIndexOfTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "time");
        final int _columnIndexOfUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "url");
        final int _columnIndexOfJson = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "json");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfLogo = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "logo");
        final int _columnIndexOfHome = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "home");
        final int _columnIndexOfParse = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "parse");
        final List<Config> _result = new ArrayList<Config>();
        while (_stmt.step()) {
          final Config _item;
          _item = new Config();
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          _item.setId(_tmpId);
          final int _tmpType;
          _tmpType = (int) (_stmt.getLong(_columnIndexOfType));
          _item.setType(_tmpType);
          final long _tmpTime;
          _tmpTime = _stmt.getLong(_columnIndexOfTime);
          _item.setTime(_tmpTime);
          final String _tmpUrl;
          if (_stmt.isNull(_columnIndexOfUrl)) {
            _tmpUrl = null;
          } else {
            _tmpUrl = _stmt.getText(_columnIndexOfUrl);
          }
          _item.setUrl(_tmpUrl);
          final String _tmpJson;
          if (_stmt.isNull(_columnIndexOfJson)) {
            _tmpJson = null;
          } else {
            _tmpJson = _stmt.getText(_columnIndexOfJson);
          }
          _item.setJson(_tmpJson);
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          _item.setName(_tmpName);
          final String _tmpLogo;
          if (_stmt.isNull(_columnIndexOfLogo)) {
            _tmpLogo = null;
          } else {
            _tmpLogo = _stmt.getText(_columnIndexOfLogo);
          }
          _item.setLogo(_tmpLogo);
          final String _tmpHome;
          if (_stmt.isNull(_columnIndexOfHome)) {
            _tmpHome = null;
          } else {
            _tmpHome = _stmt.getText(_columnIndexOfHome);
          }
          _item.setHome(_tmpHome);
          final String _tmpParse;
          if (_stmt.isNull(_columnIndexOfParse)) {
            _tmpParse = null;
          } else {
            _tmpParse = _stmt.getText(_columnIndexOfParse);
          }
          _item.setParse(_tmpParse);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<Config> findUrlByType(final int type) {
    final String _sql = "SELECT id, name, url, type, time FROM Config WHERE type = ? ORDER BY time DESC";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, type);
        final int _columnIndexOfId = 0;
        final int _columnIndexOfName = 1;
        final int _columnIndexOfUrl = 2;
        final int _columnIndexOfType = 3;
        final int _columnIndexOfTime = 4;
        final List<Config> _result = new ArrayList<Config>();
        while (_stmt.step()) {
          final Config _item;
          _item = new Config();
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          _item.setId(_tmpId);
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          _item.setName(_tmpName);
          final String _tmpUrl;
          if (_stmt.isNull(_columnIndexOfUrl)) {
            _tmpUrl = null;
          } else {
            _tmpUrl = _stmt.getText(_columnIndexOfUrl);
          }
          _item.setUrl(_tmpUrl);
          final int _tmpType;
          _tmpType = (int) (_stmt.getLong(_columnIndexOfType));
          _item.setType(_tmpType);
          final long _tmpTime;
          _tmpTime = _stmt.getLong(_columnIndexOfTime);
          _item.setTime(_tmpTime);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public Config findById(final int id) {
    final String _sql = "SELECT * FROM Config WHERE id = ?";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "type");
        final int _columnIndexOfTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "time");
        final int _columnIndexOfUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "url");
        final int _columnIndexOfJson = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "json");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfLogo = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "logo");
        final int _columnIndexOfHome = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "home");
        final int _columnIndexOfParse = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "parse");
        final Config _result;
        if (_stmt.step()) {
          _result = new Config();
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          _result.setId(_tmpId);
          final int _tmpType;
          _tmpType = (int) (_stmt.getLong(_columnIndexOfType));
          _result.setType(_tmpType);
          final long _tmpTime;
          _tmpTime = _stmt.getLong(_columnIndexOfTime);
          _result.setTime(_tmpTime);
          final String _tmpUrl;
          if (_stmt.isNull(_columnIndexOfUrl)) {
            _tmpUrl = null;
          } else {
            _tmpUrl = _stmt.getText(_columnIndexOfUrl);
          }
          _result.setUrl(_tmpUrl);
          final String _tmpJson;
          if (_stmt.isNull(_columnIndexOfJson)) {
            _tmpJson = null;
          } else {
            _tmpJson = _stmt.getText(_columnIndexOfJson);
          }
          _result.setJson(_tmpJson);
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          _result.setName(_tmpName);
          final String _tmpLogo;
          if (_stmt.isNull(_columnIndexOfLogo)) {
            _tmpLogo = null;
          } else {
            _tmpLogo = _stmt.getText(_columnIndexOfLogo);
          }
          _result.setLogo(_tmpLogo);
          final String _tmpHome;
          if (_stmt.isNull(_columnIndexOfHome)) {
            _tmpHome = null;
          } else {
            _tmpHome = _stmt.getText(_columnIndexOfHome);
          }
          _result.setHome(_tmpHome);
          final String _tmpParse;
          if (_stmt.isNull(_columnIndexOfParse)) {
            _tmpParse = null;
          } else {
            _tmpParse = _stmt.getText(_columnIndexOfParse);
          }
          _result.setParse(_tmpParse);
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public Config findOne(final int type) {
    final String _sql = "SELECT * FROM Config WHERE type = ? ORDER BY time DESC LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, type);
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "type");
        final int _columnIndexOfTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "time");
        final int _columnIndexOfUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "url");
        final int _columnIndexOfJson = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "json");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfLogo = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "logo");
        final int _columnIndexOfHome = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "home");
        final int _columnIndexOfParse = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "parse");
        final Config _result;
        if (_stmt.step()) {
          _result = new Config();
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          _result.setId(_tmpId);
          final int _tmpType;
          _tmpType = (int) (_stmt.getLong(_columnIndexOfType));
          _result.setType(_tmpType);
          final long _tmpTime;
          _tmpTime = _stmt.getLong(_columnIndexOfTime);
          _result.setTime(_tmpTime);
          final String _tmpUrl;
          if (_stmt.isNull(_columnIndexOfUrl)) {
            _tmpUrl = null;
          } else {
            _tmpUrl = _stmt.getText(_columnIndexOfUrl);
          }
          _result.setUrl(_tmpUrl);
          final String _tmpJson;
          if (_stmt.isNull(_columnIndexOfJson)) {
            _tmpJson = null;
          } else {
            _tmpJson = _stmt.getText(_columnIndexOfJson);
          }
          _result.setJson(_tmpJson);
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          _result.setName(_tmpName);
          final String _tmpLogo;
          if (_stmt.isNull(_columnIndexOfLogo)) {
            _tmpLogo = null;
          } else {
            _tmpLogo = _stmt.getText(_columnIndexOfLogo);
          }
          _result.setLogo(_tmpLogo);
          final String _tmpHome;
          if (_stmt.isNull(_columnIndexOfHome)) {
            _tmpHome = null;
          } else {
            _tmpHome = _stmt.getText(_columnIndexOfHome);
          }
          _result.setHome(_tmpHome);
          final String _tmpParse;
          if (_stmt.isNull(_columnIndexOfParse)) {
            _tmpParse = null;
          } else {
            _tmpParse = _stmt.getText(_columnIndexOfParse);
          }
          _result.setParse(_tmpParse);
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public Config find(final String url, final int type) {
    final String _sql = "SELECT * FROM Config WHERE url = ? AND type = ?";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (url == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, url);
        }
        _argIndex = 2;
        _stmt.bindLong(_argIndex, type);
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "type");
        final int _columnIndexOfTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "time");
        final int _columnIndexOfUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "url");
        final int _columnIndexOfJson = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "json");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfLogo = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "logo");
        final int _columnIndexOfHome = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "home");
        final int _columnIndexOfParse = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "parse");
        final Config _result;
        if (_stmt.step()) {
          _result = new Config();
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          _result.setId(_tmpId);
          final int _tmpType;
          _tmpType = (int) (_stmt.getLong(_columnIndexOfType));
          _result.setType(_tmpType);
          final long _tmpTime;
          _tmpTime = _stmt.getLong(_columnIndexOfTime);
          _result.setTime(_tmpTime);
          final String _tmpUrl;
          if (_stmt.isNull(_columnIndexOfUrl)) {
            _tmpUrl = null;
          } else {
            _tmpUrl = _stmt.getText(_columnIndexOfUrl);
          }
          _result.setUrl(_tmpUrl);
          final String _tmpJson;
          if (_stmt.isNull(_columnIndexOfJson)) {
            _tmpJson = null;
          } else {
            _tmpJson = _stmt.getText(_columnIndexOfJson);
          }
          _result.setJson(_tmpJson);
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          _result.setName(_tmpName);
          final String _tmpLogo;
          if (_stmt.isNull(_columnIndexOfLogo)) {
            _tmpLogo = null;
          } else {
            _tmpLogo = _stmt.getText(_columnIndexOfLogo);
          }
          _result.setLogo(_tmpLogo);
          final String _tmpHome;
          if (_stmt.isNull(_columnIndexOfHome)) {
            _tmpHome = null;
          } else {
            _tmpHome = _stmt.getText(_columnIndexOfHome);
          }
          _result.setHome(_tmpHome);
          final String _tmpParse;
          if (_stmt.isNull(_columnIndexOfParse)) {
            _tmpParse = null;
          } else {
            _tmpParse = _stmt.getText(_columnIndexOfParse);
          }
          _result.setParse(_tmpParse);
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void delete(final String url, final int type) {
    final String _sql = "DELETE FROM Config WHERE url = ? AND type = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (url == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, url);
        }
        _argIndex = 2;
        _stmt.bindLong(_argIndex, type);
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void delete(final String url) {
    final String _sql = "DELETE FROM Config WHERE url = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (url == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, url);
        }
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void delete() {
    final String _sql = "DELETE FROM Config";
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
