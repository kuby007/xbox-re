package com.fongmi.android.tv.db.dao;

import androidx.annotation.NonNull;
import androidx.room.EntityDeleteOrUpdateAdapter;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import com.fongmi.android.tv.bean.Live;
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
public final class LiveDao_Impl extends LiveDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<Live> __insertAdapterOfLive;

  private final EntityDeleteOrUpdateAdapter<Live> __updateAdapterOfLive;

  private final EntityDeleteOrUpdateAdapter<Live> __updateAdapterOfLive_1;

  public LiveDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfLive = new EntityInsertAdapter<Live>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `Live` (`name`,`keep`,`boot`,`pass`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Live entity) {
        if (entity.getName() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getName());
        }
        if (entity.getKeep() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getKeep());
        }
        final int _tmp = entity.isBoot() ? 1 : 0;
        statement.bindLong(3, _tmp);
        final int _tmp_1 = entity.isPass() ? 1 : 0;
        statement.bindLong(4, _tmp_1);
      }
    };
    this.__updateAdapterOfLive = new EntityDeleteOrUpdateAdapter<Live>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR REPLACE `Live` SET `name` = ?,`keep` = ?,`boot` = ?,`pass` = ? WHERE `name` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Live entity) {
        if (entity.getName() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getName());
        }
        if (entity.getKeep() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getKeep());
        }
        final int _tmp = entity.isBoot() ? 1 : 0;
        statement.bindLong(3, _tmp);
        final int _tmp_1 = entity.isPass() ? 1 : 0;
        statement.bindLong(4, _tmp_1);
        if (entity.getName() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getName());
        }
      }
    };
    this.__updateAdapterOfLive_1 = new EntityDeleteOrUpdateAdapter<Live>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `Live` SET `name` = ?,`keep` = ?,`boot` = ?,`pass` = ? WHERE `name` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Live entity) {
        if (entity.getName() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getName());
        }
        if (entity.getKeep() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getKeep());
        }
        final int _tmp = entity.isBoot() ? 1 : 0;
        statement.bindLong(3, _tmp);
        final int _tmp_1 = entity.isPass() ? 1 : 0;
        statement.bindLong(4, _tmp_1);
        if (entity.getName() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getName());
        }
      }
    };
  }

  @Override
  public Long insert(final Live item) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfLive.insertAndReturnId(_connection, item);
    });
  }

  @Override
  public List<Long> insert(final List<Live> items) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfLive.insertAndReturnIdsList(_connection, items);
    });
  }

  @Override
  public void update(final Live item) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfLive.handle(_connection, item);
      return null;
    });
  }

  @Override
  public void update(final List<Live> items) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfLive_1.handleMultiple(_connection, items);
      return null;
    });
  }

  @Override
  public void insertOrUpdate(final Live item) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      LiveDao_Impl.super.insertOrUpdate(item);
      return Unit.INSTANCE;
    });
  }

  @Override
  public void insertOrUpdate(final List<Live> items) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      LiveDao_Impl.super.insertOrUpdate(items);
      return Unit.INSTANCE;
    });
  }

  @Override
  public List<Live> findAll() {
    final String _sql = "SELECT * FROM Live";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfKeep = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "keep");
        final int _columnIndexOfBoot = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "boot");
        final int _columnIndexOfPass = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "pass");
        final List<Live> _result = new ArrayList<Live>();
        while (_stmt.step()) {
          final Live _item;
          _item = new Live();
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          _item.setName(_tmpName);
          final String _tmpKeep;
          if (_stmt.isNull(_columnIndexOfKeep)) {
            _tmpKeep = null;
          } else {
            _tmpKeep = _stmt.getText(_columnIndexOfKeep);
          }
          _item.setKeep(_tmpKeep);
          final boolean _tmpBoot;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfBoot));
          _tmpBoot = _tmp != 0;
          _item.setBoot(_tmpBoot);
          final boolean _tmpPass;
          final int _tmp_1;
          _tmp_1 = (int) (_stmt.getLong(_columnIndexOfPass));
          _tmpPass = _tmp_1 != 0;
          _item.setPass(_tmpPass);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public Live find(final String name) {
    final String _sql = "SELECT * FROM Live WHERE name = ?";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (name == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, name);
        }
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfKeep = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "keep");
        final int _columnIndexOfBoot = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "boot");
        final int _columnIndexOfPass = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "pass");
        final Live _result;
        if (_stmt.step()) {
          _result = new Live();
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          _result.setName(_tmpName);
          final String _tmpKeep;
          if (_stmt.isNull(_columnIndexOfKeep)) {
            _tmpKeep = null;
          } else {
            _tmpKeep = _stmt.getText(_columnIndexOfKeep);
          }
          _result.setKeep(_tmpKeep);
          final boolean _tmpBoot;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfBoot));
          _tmpBoot = _tmp != 0;
          _result.setBoot(_tmpBoot);
          final boolean _tmpPass;
          final int _tmp_1;
          _tmp_1 = (int) (_stmt.getLong(_columnIndexOfPass));
          _tmpPass = _tmp_1 != 0;
          _result.setPass(_tmpPass);
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
  public void delete() {
    final String _sql = "DELETE FROM Live";
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
