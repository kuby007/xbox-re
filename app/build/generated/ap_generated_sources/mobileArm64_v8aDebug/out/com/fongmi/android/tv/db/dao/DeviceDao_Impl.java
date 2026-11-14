package com.fongmi.android.tv.db.dao;

import androidx.annotation.NonNull;
import androidx.room.EntityDeleteOrUpdateAdapter;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import com.fongmi.android.tv.bean.Device;
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
public final class DeviceDao_Impl extends DeviceDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<Device> __insertAdapterOfDevice;

  private final EntityDeleteOrUpdateAdapter<Device> __updateAdapterOfDevice;

  private final EntityDeleteOrUpdateAdapter<Device> __updateAdapterOfDevice_1;

  public DeviceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfDevice = new EntityInsertAdapter<Device>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `Device` (`id`,`uuid`,`name`,`ip`,`type`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Device entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindLong(1, entity.getId());
        }
        if (entity.getUuid() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getUuid());
        }
        if (entity.getName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getName());
        }
        if (entity.getIp() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getIp());
        }
        statement.bindLong(5, entity.getType());
      }
    };
    this.__updateAdapterOfDevice = new EntityDeleteOrUpdateAdapter<Device>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR REPLACE `Device` SET `id` = ?,`uuid` = ?,`name` = ?,`ip` = ?,`type` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Device entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindLong(1, entity.getId());
        }
        if (entity.getUuid() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getUuid());
        }
        if (entity.getName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getName());
        }
        if (entity.getIp() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getIp());
        }
        statement.bindLong(5, entity.getType());
        if (entity.getId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getId());
        }
      }
    };
    this.__updateAdapterOfDevice_1 = new EntityDeleteOrUpdateAdapter<Device>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `Device` SET `id` = ?,`uuid` = ?,`name` = ?,`ip` = ?,`type` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final Device entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindLong(1, entity.getId());
        }
        if (entity.getUuid() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getUuid());
        }
        if (entity.getName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getName());
        }
        if (entity.getIp() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getIp());
        }
        statement.bindLong(5, entity.getType());
        if (entity.getId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getId());
        }
      }
    };
  }

  @Override
  public Long insert(final Device item) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfDevice.insertAndReturnId(_connection, item);
    });
  }

  @Override
  public List<Long> insert(final List<Device> items) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfDevice.insertAndReturnIdsList(_connection, items);
    });
  }

  @Override
  public void update(final Device item) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfDevice.handle(_connection, item);
      return null;
    });
  }

  @Override
  public void update(final List<Device> items) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfDevice_1.handleMultiple(_connection, items);
      return null;
    });
  }

  @Override
  public void insertOrUpdate(final Device item) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      DeviceDao_Impl.super.insertOrUpdate(item);
      return Unit.INSTANCE;
    });
  }

  @Override
  public void insertOrUpdate(final List<Device> items) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      DeviceDao_Impl.super.insertOrUpdate(items);
      return Unit.INSTANCE;
    });
  }

  @Override
  public List<Device> findAll() {
    final String _sql = "SELECT * FROM Device";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfUuid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "uuid");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfIp = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "ip");
        final int _columnIndexOfType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "type");
        final List<Device> _result = new ArrayList<Device>();
        while (_stmt.step()) {
          final Device _item;
          _item = new Device();
          final Integer _tmpId;
          if (_stmt.isNull(_columnIndexOfId)) {
            _tmpId = null;
          } else {
            _tmpId = (int) (_stmt.getLong(_columnIndexOfId));
          }
          _item.setId(_tmpId);
          final String _tmpUuid;
          if (_stmt.isNull(_columnIndexOfUuid)) {
            _tmpUuid = null;
          } else {
            _tmpUuid = _stmt.getText(_columnIndexOfUuid);
          }
          _item.setUuid(_tmpUuid);
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          _item.setName(_tmpName);
          final String _tmpIp;
          if (_stmt.isNull(_columnIndexOfIp)) {
            _tmpIp = null;
          } else {
            _tmpIp = _stmt.getText(_columnIndexOfIp);
          }
          _item.setIp(_tmpIp);
          final int _tmpType;
          _tmpType = (int) (_stmt.getLong(_columnIndexOfType));
          _item.setType(_tmpType);
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
    final String _sql = "DELETE FROM Device";
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
  public void delete(final String uuid) {
    final String _sql = "DELETE FROM Device WHERE uuid = ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (uuid == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, uuid);
        }
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
