package com.fongmi.android.tv.db.dao;

import androidx.annotation.NonNull;
import androidx.room.EntityDeleteOrUpdateAdapter;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import com.fongmi.android.tv.bean.Keep;
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
public final class KeepDao_Impl extends KeepDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<Keep> __insertAdapterOfKeep;

  private final EntityDeleteOrUpdateAdapter<Keep> __updateAdapterOfKeep;

  private final EntityDeleteOrUpdateAdapter<Keep> __updateAdapterOfKeep_1;

  public KeepDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfKeep = new EntityInsertAdapter<Keep>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `Keep` (`key`,`siteName`,`vodName`,`vodPic`,`createTime`,`type`,`cid`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Keep entity) {
        if (entity.getKey() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getKey());
        }
        if (entity.getSiteName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getSiteName());
        }
        if (entity.getVodName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getVodName());
        }
        if (entity.getVodPic() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getVodPic());
        }
        statement.bindLong(5, entity.getCreateTime());
        statement.bindLong(6, entity.getType());
        statement.bindLong(7, entity.getCid());
      }
    };
    this.__updateAdapterOfKeep = new EntityDeleteOrUpdateAdapter<Keep>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR REPLACE `Keep` SET `key` = ?,`siteName` = ?,`vodName` = ?,`vodPic` = ?,`createTime` = ?,`type` = ?,`cid` = ? WHERE `key` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Keep entity) {
        if (entity.getKey() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getKey());
        }
        if (entity.getSiteName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getSiteName());
        }
        if (entity.getVodName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getVodName());
        }
        if (entity.getVodPic() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getVodPic());
        }
        statement.bindLong(5, entity.getCreateTime());
        statement.bindLong(6, entity.getType());
        statement.bindLong(7, entity.getCid());
        if (entity.getKey() == null) {
          statement.bindNull(8);
        } else {
          statement.bindText(8, entity.getKey());
        }
      }
    };
    this.__updateAdapterOfKeep_1 = new EntityDeleteOrUpdateAdapter<Keep>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `Keep` SET `key` = ?,`siteName` = ?,`vodName` = ?,`vodPic` = ?,`createTime` = ?,`type` = ?,`cid` = ? WHERE `key` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Keep entity) {
        if (entity.getKey() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getKey());
        }
        if (entity.getSiteName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getSiteName());
        }
        if (entity.getVodName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getVodName());
        }
        if (entity.getVodPic() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getVodPic());
        }
        statement.bindLong(5, entity.getCreateTime());
        statement.bindLong(6, entity.getType());
        statement.bindLong(7, entity.getCid());
        if (entity.getKey() == null) {
          statement.bindNull(8);
        } else {
          statement.bindText(8, entity.getKey());
        }
      }
    };
  }

  @Override
  public Long insert(final Keep item) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfKeep.insertAndReturnId(_connection, item);
    });
  }

  @Override
  public List<Long> insert(final List<Keep> items) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfKeep.insertAndReturnIdsList(_connection, items);
    });
  }

  @Override
  public void update(final Keep item) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfKeep.handle(_connection, item);
      return null;
    });
  }

  @Override
  public void update(final List<Keep> items) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfKeep_1.handleMultiple(_connection, items);
      return null;
    });
  }

  @Override
  public void insertOrUpdate(final Keep item) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      KeepDao_Impl.super.insertOrUpdate(item);
      return Unit.INSTANCE;
    });
  }

  @Override
  public void insertOrUpdate(final List<Keep> items) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      KeepDao_Impl.super.insertOrUpdate(items);
      return Unit.INSTANCE;
    });
  }

  @Override
  public List<Keep> findAll() {
    final String _sql = "SELECT * FROM Keep";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "key");
        final int _columnIndexOfSiteName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "siteName");
        final int _columnIndexOfVodName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodName");
        final int _columnIndexOfVodPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodPic");
        final int _columnIndexOfCreateTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createTime");
        final int _columnIndexOfType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "type");
        final int _columnIndexOfCid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "cid");
        final List<Keep> _result = new ArrayList<Keep>();
        while (_stmt.step()) {
          final Keep _item;
          _item = new Keep();
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfKey)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfKey);
          }
          _item.setKey(_tmpKey);
          final String _tmpSiteName;
          if (_stmt.isNull(_columnIndexOfSiteName)) {
            _tmpSiteName = null;
          } else {
            _tmpSiteName = _stmt.getText(_columnIndexOfSiteName);
          }
          _item.setSiteName(_tmpSiteName);
          final String _tmpVodName;
          if (_stmt.isNull(_columnIndexOfVodName)) {
            _tmpVodName = null;
          } else {
            _tmpVodName = _stmt.getText(_columnIndexOfVodName);
          }
          _item.setVodName(_tmpVodName);
          final String _tmpVodPic;
          if (_stmt.isNull(_columnIndexOfVodPic)) {
            _tmpVodPic = null;
          } else {
            _tmpVodPic = _stmt.getText(_columnIndexOfVodPic);
          }
          _item.setVodPic(_tmpVodPic);
          final long _tmpCreateTime;
          _tmpCreateTime = _stmt.getLong(_columnIndexOfCreateTime);
          _item.setCreateTime(_tmpCreateTime);
          final int _tmpType;
          _tmpType = (int) (_stmt.getLong(_columnIndexOfType));
          _item.setType(_tmpType);
          final int _tmpCid;
          _tmpCid = (int) (_stmt.getLong(_columnIndexOfCid));
          _item.setCid(_tmpCid);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<Keep> getVod() {
    final String _sql = "SELECT * FROM Keep WHERE type = 0 ORDER BY createTime DESC";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "key");
        final int _columnIndexOfSiteName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "siteName");
        final int _columnIndexOfVodName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodName");
        final int _columnIndexOfVodPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodPic");
        final int _columnIndexOfCreateTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createTime");
        final int _columnIndexOfType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "type");
        final int _columnIndexOfCid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "cid");
        final List<Keep> _result = new ArrayList<Keep>();
        while (_stmt.step()) {
          final Keep _item;
          _item = new Keep();
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfKey)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfKey);
          }
          _item.setKey(_tmpKey);
          final String _tmpSiteName;
          if (_stmt.isNull(_columnIndexOfSiteName)) {
            _tmpSiteName = null;
          } else {
            _tmpSiteName = _stmt.getText(_columnIndexOfSiteName);
          }
          _item.setSiteName(_tmpSiteName);
          final String _tmpVodName;
          if (_stmt.isNull(_columnIndexOfVodName)) {
            _tmpVodName = null;
          } else {
            _tmpVodName = _stmt.getText(_columnIndexOfVodName);
          }
          _item.setVodName(_tmpVodName);
          final String _tmpVodPic;
          if (_stmt.isNull(_columnIndexOfVodPic)) {
            _tmpVodPic = null;
          } else {
            _tmpVodPic = _stmt.getText(_columnIndexOfVodPic);
          }
          _item.setVodPic(_tmpVodPic);
          final long _tmpCreateTime;
          _tmpCreateTime = _stmt.getLong(_columnIndexOfCreateTime);
          _item.setCreateTime(_tmpCreateTime);
          final int _tmpType;
          _tmpType = (int) (_stmt.getLong(_columnIndexOfType));
          _item.setType(_tmpType);
          final int _tmpCid;
          _tmpCid = (int) (_stmt.getLong(_columnIndexOfCid));
          _item.setCid(_tmpCid);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<Keep> getLive() {
    final String _sql = "SELECT * FROM Keep WHERE type = 1 ORDER BY createTime DESC";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "key");
        final int _columnIndexOfSiteName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "siteName");
        final int _columnIndexOfVodName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodName");
        final int _columnIndexOfVodPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodPic");
        final int _columnIndexOfCreateTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createTime");
        final int _columnIndexOfType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "type");
        final int _columnIndexOfCid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "cid");
        final List<Keep> _result = new ArrayList<Keep>();
        while (_stmt.step()) {
          final Keep _item;
          _item = new Keep();
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfKey)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfKey);
          }
          _item.setKey(_tmpKey);
          final String _tmpSiteName;
          if (_stmt.isNull(_columnIndexOfSiteName)) {
            _tmpSiteName = null;
          } else {
            _tmpSiteName = _stmt.getText(_columnIndexOfSiteName);
          }
          _item.setSiteName(_tmpSiteName);
          final String _tmpVodName;
          if (_stmt.isNull(_columnIndexOfVodName)) {
            _tmpVodName = null;
          } else {
            _tmpVodName = _stmt.getText(_columnIndexOfVodName);
          }
          _item.setVodName(_tmpVodName);
          final String _tmpVodPic;
          if (_stmt.isNull(_columnIndexOfVodPic)) {
            _tmpVodPic = null;
          } else {
            _tmpVodPic = _stmt.getText(_columnIndexOfVodPic);
          }
          _item.setVodPic(_tmpVodPic);
          final long _tmpCreateTime;
          _tmpCreateTime = _stmt.getLong(_columnIndexOfCreateTime);
          _item.setCreateTime(_tmpCreateTime);
          final int _tmpType;
          _tmpType = (int) (_stmt.getLong(_columnIndexOfType));
          _item.setType(_tmpType);
          final int _tmpCid;
          _tmpCid = (int) (_stmt.getLong(_columnIndexOfCid));
          _item.setCid(_tmpCid);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public Keep find(final int cid, final String key) {
    final String _sql = "SELECT * FROM Keep WHERE type = 0 AND cid = ? AND `key` = ?";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cid);
        _argIndex = 2;
        if (key == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, key);
        }
        final int _columnIndexOfKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "key");
        final int _columnIndexOfSiteName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "siteName");
        final int _columnIndexOfVodName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodName");
        final int _columnIndexOfVodPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodPic");
        final int _columnIndexOfCreateTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createTime");
        final int _columnIndexOfType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "type");
        final int _columnIndexOfCid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "cid");
        final Keep _result;
        if (_stmt.step()) {
          _result = new Keep();
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfKey)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfKey);
          }
          _result.setKey(_tmpKey);
          final String _tmpSiteName;
          if (_stmt.isNull(_columnIndexOfSiteName)) {
            _tmpSiteName = null;
          } else {
            _tmpSiteName = _stmt.getText(_columnIndexOfSiteName);
          }
          _result.setSiteName(_tmpSiteName);
          final String _tmpVodName;
          if (_stmt.isNull(_columnIndexOfVodName)) {
            _tmpVodName = null;
          } else {
            _tmpVodName = _stmt.getText(_columnIndexOfVodName);
          }
          _result.setVodName(_tmpVodName);
          final String _tmpVodPic;
          if (_stmt.isNull(_columnIndexOfVodPic)) {
            _tmpVodPic = null;
          } else {
            _tmpVodPic = _stmt.getText(_columnIndexOfVodPic);
          }
          _result.setVodPic(_tmpVodPic);
          final long _tmpCreateTime;
          _tmpCreateTime = _stmt.getLong(_columnIndexOfCreateTime);
          _result.setCreateTime(_tmpCreateTime);
          final int _tmpType;
          _tmpType = (int) (_stmt.getLong(_columnIndexOfType));
          _result.setType(_tmpType);
          final int _tmpCid;
          _tmpCid = (int) (_stmt.getLong(_columnIndexOfCid));
          _result.setCid(_tmpCid);
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
  public Keep find(final String key) {
    final String _sql = "SELECT * FROM Keep WHERE type = 1 AND `key` = ?";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (key == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, key);
        }
        final int _columnIndexOfKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "key");
        final int _columnIndexOfSiteName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "siteName");
        final int _columnIndexOfVodName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodName");
        final int _columnIndexOfVodPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodPic");
        final int _columnIndexOfCreateTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createTime");
        final int _columnIndexOfType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "type");
        final int _columnIndexOfCid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "cid");
        final Keep _result;
        if (_stmt.step()) {
          _result = new Keep();
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfKey)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfKey);
          }
          _result.setKey(_tmpKey);
          final String _tmpSiteName;
          if (_stmt.isNull(_columnIndexOfSiteName)) {
            _tmpSiteName = null;
          } else {
            _tmpSiteName = _stmt.getText(_columnIndexOfSiteName);
          }
          _result.setSiteName(_tmpSiteName);
          final String _tmpVodName;
          if (_stmt.isNull(_columnIndexOfVodName)) {
            _tmpVodName = null;
          } else {
            _tmpVodName = _stmt.getText(_columnIndexOfVodName);
          }
          _result.setVodName(_tmpVodName);
          final String _tmpVodPic;
          if (_stmt.isNull(_columnIndexOfVodPic)) {
            _tmpVodPic = null;
          } else {
            _tmpVodPic = _stmt.getText(_columnIndexOfVodPic);
          }
          _result.setVodPic(_tmpVodPic);
          final long _tmpCreateTime;
          _tmpCreateTime = _stmt.getLong(_columnIndexOfCreateTime);
          _result.setCreateTime(_tmpCreateTime);
          final int _tmpType;
          _tmpType = (int) (_stmt.getLong(_columnIndexOfType));
          _result.setType(_tmpType);
          final int _tmpCid;
          _tmpCid = (int) (_stmt.getLong(_columnIndexOfCid));
          _result.setCid(_tmpCid);
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
  public void delete(final String key) {
    final String _sql = "DELETE FROM Keep WHERE type = 1 AND `key` = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (key == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, key);
        }
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void delete(final int cid, final String key) {
    final String _sql = "DELETE FROM Keep WHERE type = 0 AND cid = ? AND `key` = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cid);
        _argIndex = 2;
        if (key == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, key);
        }
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void delete(final int cid) {
    final String _sql = "DELETE FROM Keep WHERE type = 0 AND cid = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cid);
        _stmt.step();
        return null;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void delete() {
    final String _sql = "DELETE FROM Keep WHERE type = 0";
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

  @Override
  public void deleteAll() {
    final String _sql = "DELETE FROM Keep";
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
