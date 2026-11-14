package com.fongmi.android.tv.db.dao;

import androidx.annotation.NonNull;
import androidx.room.EntityDeleteOrUpdateAdapter;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import com.fongmi.android.tv.bean.Track;
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
public final class TrackDao_Impl extends TrackDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<Track> __insertAdapterOfTrack;

  private final EntityInsertAdapter<Track> __insertAdapterOfTrack_1;

  private final EntityDeleteOrUpdateAdapter<Track> __updateAdapterOfTrack;

  private final EntityDeleteOrUpdateAdapter<Track> __updateAdapterOfTrack_1;

  public TrackDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfTrack = new EntityInsertAdapter<Track>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `Track` (`id`,`type`,`key`,`name`,`format`,`selected`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Track entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getType());
        if (entity.getKey() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getKey());
        }
        if (entity.getName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getName());
        }
        if (entity.getFormat() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getFormat());
        }
        final int _tmp = entity.isSelected() ? 1 : 0;
        statement.bindLong(6, _tmp);
      }
    };
    this.__insertAdapterOfTrack_1 = new EntityInsertAdapter<Track>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `Track` (`id`,`type`,`key`,`name`,`format`,`selected`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Track entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getType());
        if (entity.getKey() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getKey());
        }
        if (entity.getName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getName());
        }
        if (entity.getFormat() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getFormat());
        }
        final int _tmp = entity.isSelected() ? 1 : 0;
        statement.bindLong(6, _tmp);
      }
    };
    this.__updateAdapterOfTrack = new EntityDeleteOrUpdateAdapter<Track>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR REPLACE `Track` SET `id` = ?,`type` = ?,`key` = ?,`name` = ?,`format` = ?,`selected` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Track entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getType());
        if (entity.getKey() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getKey());
        }
        if (entity.getName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getName());
        }
        if (entity.getFormat() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getFormat());
        }
        final int _tmp = entity.isSelected() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindLong(7, entity.getId());
      }
    };
    this.__updateAdapterOfTrack_1 = new EntityDeleteOrUpdateAdapter<Track>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `Track` SET `id` = ?,`type` = ?,`key` = ?,`name` = ?,`format` = ?,`selected` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Track entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getType());
        if (entity.getKey() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getKey());
        }
        if (entity.getName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getName());
        }
        if (entity.getFormat() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getFormat());
        }
        final int _tmp = entity.isSelected() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindLong(7, entity.getId());
      }
    };
  }

  @Override
  public List<Long> insert(final List<Track> items) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfTrack.insertAndReturnIdsList(_connection, items);
    });
  }

  @Override
  public Long insert(final Track item) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfTrack_1.insertAndReturnId(_connection, item);
    });
  }

  @Override
  public void update(final Track item) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfTrack.handle(_connection, item);
      return null;
    });
  }

  @Override
  public void update(final List<Track> items) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfTrack_1.handleMultiple(_connection, items);
      return null;
    });
  }

  @Override
  public void insertOrUpdate(final Track item) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      TrackDao_Impl.super.insertOrUpdate(item);
      return Unit.INSTANCE;
    });
  }

  @Override
  public void insertOrUpdate(final List<Track> items) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      TrackDao_Impl.super.insertOrUpdate(items);
      return Unit.INSTANCE;
    });
  }

  @Override
  public List<Track> findAll() {
    final String _sql = "SELECT * FROM Track";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "type");
        final int _columnIndexOfKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "key");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfFormat = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "format");
        final int _columnIndexOfSelected = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "selected");
        final List<Track> _result = new ArrayList<Track>();
        while (_stmt.step()) {
          final Track _item;
          final int _tmpType;
          _tmpType = (int) (_stmt.getLong(_columnIndexOfType));
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          final String _tmpFormat;
          if (_stmt.isNull(_columnIndexOfFormat)) {
            _tmpFormat = null;
          } else {
            _tmpFormat = _stmt.getText(_columnIndexOfFormat);
          }
          _item = new Track(_tmpType,_tmpName,_tmpFormat);
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          _item.setId(_tmpId);
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfKey)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfKey);
          }
          _item.setKey(_tmpKey);
          final boolean _tmpSelected;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfSelected));
          _tmpSelected = _tmp != 0;
          _item.setSelected(_tmpSelected);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<Track> find(final String key) {
    final String _sql = "SELECT * FROM Track WHERE `key` = ?";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (key == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, key);
        }
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "type");
        final int _columnIndexOfKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "key");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfFormat = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "format");
        final int _columnIndexOfSelected = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "selected");
        final List<Track> _result = new ArrayList<Track>();
        while (_stmt.step()) {
          final Track _item;
          final int _tmpType;
          _tmpType = (int) (_stmt.getLong(_columnIndexOfType));
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          final String _tmpFormat;
          if (_stmt.isNull(_columnIndexOfFormat)) {
            _tmpFormat = null;
          } else {
            _tmpFormat = _stmt.getText(_columnIndexOfFormat);
          }
          _item = new Track(_tmpType,_tmpName,_tmpFormat);
          final int _tmpId;
          _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          _item.setId(_tmpId);
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfKey)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfKey);
          }
          _item.setKey(_tmpKey);
          final boolean _tmpSelected;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfSelected));
          _tmpSelected = _tmp != 0;
          _item.setSelected(_tmpSelected);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public void delete(final String key) {
    final String _sql = "DELETE FROM Track WHERE `key` = ?";
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
  public void deleteAll() {
    final String _sql = "DELETE FROM Track";
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
