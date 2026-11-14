package com.fongmi.android.tv.db.dao;

import androidx.annotation.NonNull;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import com.fongmi.android.tv.bean.PlaybackDeleteTombstone;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class PlaybackDeleteTombstoneDao_Impl extends PlaybackDeleteTombstoneDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<PlaybackDeleteTombstone> __insertAdapterOfPlaybackDeleteTombstone;

  public PlaybackDeleteTombstoneDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfPlaybackDeleteTombstone = new EntityInsertAdapter<PlaybackDeleteTombstone>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `PlaybackDeleteTombstone` (`id`,`configKey`,`scope`,`historyKey`,`siteKey`,`vodId`,`deletedAt`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement,
          final PlaybackDeleteTombstone entity) {
        if (entity.id == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.id);
        }
        if (entity.configKey == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.configKey);
        }
        if (entity.scope == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.scope);
        }
        if (entity.historyKey == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.historyKey);
        }
        if (entity.siteKey == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.siteKey);
        }
        if (entity.vodId == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.vodId);
        }
        statement.bindLong(7, entity.deletedAt);
      }
    };
  }

  @Override
  public void insertOrUpdate(final PlaybackDeleteTombstone tombstone) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfPlaybackDeleteTombstone.insert(_connection, tombstone);
      return null;
    });
  }

  @Override
  public List<PlaybackDeleteTombstone> findAll() {
    final String _sql = "SELECT * FROM PlaybackDeleteTombstone ORDER BY deletedAt DESC";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfConfigKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "configKey");
        final int _columnIndexOfScope = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "scope");
        final int _columnIndexOfHistoryKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "historyKey");
        final int _columnIndexOfSiteKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "siteKey");
        final int _columnIndexOfVodId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodId");
        final int _columnIndexOfDeletedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "deletedAt");
        final List<PlaybackDeleteTombstone> _result = new ArrayList<PlaybackDeleteTombstone>();
        while (_stmt.step()) {
          final PlaybackDeleteTombstone _item;
          _item = new PlaybackDeleteTombstone();
          if (_stmt.isNull(_columnIndexOfId)) {
            _item.id = null;
          } else {
            _item.id = _stmt.getText(_columnIndexOfId);
          }
          if (_stmt.isNull(_columnIndexOfConfigKey)) {
            _item.configKey = null;
          } else {
            _item.configKey = _stmt.getText(_columnIndexOfConfigKey);
          }
          if (_stmt.isNull(_columnIndexOfScope)) {
            _item.scope = null;
          } else {
            _item.scope = _stmt.getText(_columnIndexOfScope);
          }
          if (_stmt.isNull(_columnIndexOfHistoryKey)) {
            _item.historyKey = null;
          } else {
            _item.historyKey = _stmt.getText(_columnIndexOfHistoryKey);
          }
          if (_stmt.isNull(_columnIndexOfSiteKey)) {
            _item.siteKey = null;
          } else {
            _item.siteKey = _stmt.getText(_columnIndexOfSiteKey);
          }
          if (_stmt.isNull(_columnIndexOfVodId)) {
            _item.vodId = null;
          } else {
            _item.vodId = _stmt.getText(_columnIndexOfVodId);
          }
          _item.deletedAt = _stmt.getLong(_columnIndexOfDeletedAt);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public PlaybackDeleteTombstone find(final String id) {
    final String _sql = "SELECT * FROM PlaybackDeleteTombstone WHERE id = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (id == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, id);
        }
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfConfigKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "configKey");
        final int _columnIndexOfScope = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "scope");
        final int _columnIndexOfHistoryKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "historyKey");
        final int _columnIndexOfSiteKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "siteKey");
        final int _columnIndexOfVodId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodId");
        final int _columnIndexOfDeletedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "deletedAt");
        final PlaybackDeleteTombstone _result;
        if (_stmt.step()) {
          _result = new PlaybackDeleteTombstone();
          if (_stmt.isNull(_columnIndexOfId)) {
            _result.id = null;
          } else {
            _result.id = _stmt.getText(_columnIndexOfId);
          }
          if (_stmt.isNull(_columnIndexOfConfigKey)) {
            _result.configKey = null;
          } else {
            _result.configKey = _stmt.getText(_columnIndexOfConfigKey);
          }
          if (_stmt.isNull(_columnIndexOfScope)) {
            _result.scope = null;
          } else {
            _result.scope = _stmt.getText(_columnIndexOfScope);
          }
          if (_stmt.isNull(_columnIndexOfHistoryKey)) {
            _result.historyKey = null;
          } else {
            _result.historyKey = _stmt.getText(_columnIndexOfHistoryKey);
          }
          if (_stmt.isNull(_columnIndexOfSiteKey)) {
            _result.siteKey = null;
          } else {
            _result.siteKey = _stmt.getText(_columnIndexOfSiteKey);
          }
          if (_stmt.isNull(_columnIndexOfVodId)) {
            _result.vodId = null;
          } else {
            _result.vodId = _stmt.getText(_columnIndexOfVodId);
          }
          _result.deletedAt = _stmt.getLong(_columnIndexOfDeletedAt);
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
  public void deleteBefore(final long cutoff) {
    final String _sql = "DELETE FROM PlaybackDeleteTombstone WHERE deletedAt < ?";
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cutoff);
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
