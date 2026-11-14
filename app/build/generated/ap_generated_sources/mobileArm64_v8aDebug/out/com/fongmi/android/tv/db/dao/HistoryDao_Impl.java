package com.fongmi.android.tv.db.dao;

import androidx.annotation.NonNull;
import androidx.room.EntityDeleteOrUpdateAdapter;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteConnectionUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import com.fongmi.android.tv.bean.History;
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
public final class HistoryDao_Impl extends HistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<History> __insertAdapterOfHistory;

  private final EntityDeleteOrUpdateAdapter<History> __updateAdapterOfHistory;

  private final EntityDeleteOrUpdateAdapter<History> __updateAdapterOfHistory_1;

  public HistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfHistory = new EntityInsertAdapter<History>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `History` (`key`,`vodPic`,`wallPic`,`vodName`,`vodFlag`,`vodRemarks`,`episodeUrl`,`revSort`,`revPlay`,`createTime`,`opening`,`ending`,`position`,`duration`,`speed`,`scale`,`cid`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final History entity) {
        if (entity.getKey() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getKey());
        }
        if (entity.getVodPic() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getVodPic());
        }
        if (entity.getWallPic() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getWallPic());
        }
        if (entity.getVodName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getVodName());
        }
        if (entity.getVodFlag() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getVodFlag());
        }
        if (entity.getVodRemarks() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getVodRemarks());
        }
        if (entity.getEpisodeUrl() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getEpisodeUrl());
        }
        final int _tmp = entity.isRevSort() ? 1 : 0;
        statement.bindLong(8, _tmp);
        final int _tmp_1 = entity.isRevPlay() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        statement.bindLong(10, entity.getCreateTime());
        statement.bindLong(11, entity.getOpening());
        statement.bindLong(12, entity.getEnding());
        statement.bindLong(13, entity.getPosition());
        statement.bindLong(14, entity.getDuration());
        statement.bindDouble(15, entity.getSpeed());
        statement.bindLong(16, entity.getScale());
        statement.bindLong(17, entity.getCid());
      }
    };
    this.__updateAdapterOfHistory = new EntityDeleteOrUpdateAdapter<History>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR REPLACE `History` SET `key` = ?,`vodPic` = ?,`wallPic` = ?,`vodName` = ?,`vodFlag` = ?,`vodRemarks` = ?,`episodeUrl` = ?,`revSort` = ?,`revPlay` = ?,`createTime` = ?,`opening` = ?,`ending` = ?,`position` = ?,`duration` = ?,`speed` = ?,`scale` = ?,`cid` = ? WHERE `key` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final History entity) {
        if (entity.getKey() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getKey());
        }
        if (entity.getVodPic() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getVodPic());
        }
        if (entity.getWallPic() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getWallPic());
        }
        if (entity.getVodName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getVodName());
        }
        if (entity.getVodFlag() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getVodFlag());
        }
        if (entity.getVodRemarks() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getVodRemarks());
        }
        if (entity.getEpisodeUrl() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getEpisodeUrl());
        }
        final int _tmp = entity.isRevSort() ? 1 : 0;
        statement.bindLong(8, _tmp);
        final int _tmp_1 = entity.isRevPlay() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        statement.bindLong(10, entity.getCreateTime());
        statement.bindLong(11, entity.getOpening());
        statement.bindLong(12, entity.getEnding());
        statement.bindLong(13, entity.getPosition());
        statement.bindLong(14, entity.getDuration());
        statement.bindDouble(15, entity.getSpeed());
        statement.bindLong(16, entity.getScale());
        statement.bindLong(17, entity.getCid());
        if (entity.getKey() == null) {
          statement.bindNull(18);
        } else {
          statement.bindText(18, entity.getKey());
        }
      }
    };
    this.__updateAdapterOfHistory_1 = new EntityDeleteOrUpdateAdapter<History>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `History` SET `key` = ?,`vodPic` = ?,`wallPic` = ?,`vodName` = ?,`vodFlag` = ?,`vodRemarks` = ?,`episodeUrl` = ?,`revSort` = ?,`revPlay` = ?,`createTime` = ?,`opening` = ?,`ending` = ?,`position` = ?,`duration` = ?,`speed` = ?,`scale` = ?,`cid` = ? WHERE `key` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final History entity) {
        if (entity.getKey() == null) {
          statement.bindNull(1);
        } else {
          statement.bindText(1, entity.getKey());
        }
        if (entity.getVodPic() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getVodPic());
        }
        if (entity.getWallPic() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getWallPic());
        }
        if (entity.getVodName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindText(4, entity.getVodName());
        }
        if (entity.getVodFlag() == null) {
          statement.bindNull(5);
        } else {
          statement.bindText(5, entity.getVodFlag());
        }
        if (entity.getVodRemarks() == null) {
          statement.bindNull(6);
        } else {
          statement.bindText(6, entity.getVodRemarks());
        }
        if (entity.getEpisodeUrl() == null) {
          statement.bindNull(7);
        } else {
          statement.bindText(7, entity.getEpisodeUrl());
        }
        final int _tmp = entity.isRevSort() ? 1 : 0;
        statement.bindLong(8, _tmp);
        final int _tmp_1 = entity.isRevPlay() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        statement.bindLong(10, entity.getCreateTime());
        statement.bindLong(11, entity.getOpening());
        statement.bindLong(12, entity.getEnding());
        statement.bindLong(13, entity.getPosition());
        statement.bindLong(14, entity.getDuration());
        statement.bindDouble(15, entity.getSpeed());
        statement.bindLong(16, entity.getScale());
        statement.bindLong(17, entity.getCid());
        if (entity.getKey() == null) {
          statement.bindNull(18);
        } else {
          statement.bindText(18, entity.getKey());
        }
      }
    };
  }

  @Override
  public Long insert(final History item) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfHistory.insertAndReturnId(_connection, item);
    });
  }

  @Override
  public List<Long> insert(final List<History> items) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfHistory.insertAndReturnIdsList(_connection, items);
    });
  }

  @Override
  public void update(final History item) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfHistory.handle(_connection, item);
      return null;
    });
  }

  @Override
  public void update(final List<History> items) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __updateAdapterOfHistory_1.handleMultiple(_connection, items);
      return null;
    });
  }

  @Override
  public void insertOrUpdate(final History item) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      HistoryDao_Impl.super.insertOrUpdate(item);
      return Unit.INSTANCE;
    });
  }

  @Override
  public void insertOrUpdate(final List<History> items) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      HistoryDao_Impl.super.insertOrUpdate(items);
      return Unit.INSTANCE;
    });
  }

  @Override
  public List<History> findAll() {
    final String _sql = "SELECT * FROM History";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "key");
        final int _columnIndexOfVodPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodPic");
        final int _columnIndexOfWallPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "wallPic");
        final int _columnIndexOfVodName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodName");
        final int _columnIndexOfVodFlag = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodFlag");
        final int _columnIndexOfVodRemarks = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodRemarks");
        final int _columnIndexOfEpisodeUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "episodeUrl");
        final int _columnIndexOfRevSort = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revSort");
        final int _columnIndexOfRevPlay = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revPlay");
        final int _columnIndexOfCreateTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createTime");
        final int _columnIndexOfOpening = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "opening");
        final int _columnIndexOfEnding = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "ending");
        final int _columnIndexOfPosition = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "position");
        final int _columnIndexOfDuration = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "duration");
        final int _columnIndexOfSpeed = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "speed");
        final int _columnIndexOfScale = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "scale");
        final int _columnIndexOfCid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "cid");
        final List<History> _result = new ArrayList<History>();
        while (_stmt.step()) {
          final History _item;
          _item = new History();
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfKey)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfKey);
          }
          _item.setKey(_tmpKey);
          final String _tmpVodPic;
          if (_stmt.isNull(_columnIndexOfVodPic)) {
            _tmpVodPic = null;
          } else {
            _tmpVodPic = _stmt.getText(_columnIndexOfVodPic);
          }
          _item.setVodPic(_tmpVodPic);
          final String _tmpWallPic;
          if (_stmt.isNull(_columnIndexOfWallPic)) {
            _tmpWallPic = null;
          } else {
            _tmpWallPic = _stmt.getText(_columnIndexOfWallPic);
          }
          _item.setWallPic(_tmpWallPic);
          final String _tmpVodName;
          if (_stmt.isNull(_columnIndexOfVodName)) {
            _tmpVodName = null;
          } else {
            _tmpVodName = _stmt.getText(_columnIndexOfVodName);
          }
          _item.setVodName(_tmpVodName);
          final String _tmpVodFlag;
          if (_stmt.isNull(_columnIndexOfVodFlag)) {
            _tmpVodFlag = null;
          } else {
            _tmpVodFlag = _stmt.getText(_columnIndexOfVodFlag);
          }
          _item.setVodFlag(_tmpVodFlag);
          final String _tmpVodRemarks;
          if (_stmt.isNull(_columnIndexOfVodRemarks)) {
            _tmpVodRemarks = null;
          } else {
            _tmpVodRemarks = _stmt.getText(_columnIndexOfVodRemarks);
          }
          _item.setVodRemarks(_tmpVodRemarks);
          final String _tmpEpisodeUrl;
          if (_stmt.isNull(_columnIndexOfEpisodeUrl)) {
            _tmpEpisodeUrl = null;
          } else {
            _tmpEpisodeUrl = _stmt.getText(_columnIndexOfEpisodeUrl);
          }
          _item.setEpisodeUrl(_tmpEpisodeUrl);
          final boolean _tmpRevSort;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfRevSort));
          _tmpRevSort = _tmp != 0;
          _item.setRevSort(_tmpRevSort);
          final boolean _tmpRevPlay;
          final int _tmp_1;
          _tmp_1 = (int) (_stmt.getLong(_columnIndexOfRevPlay));
          _tmpRevPlay = _tmp_1 != 0;
          _item.setRevPlay(_tmpRevPlay);
          final long _tmpCreateTime;
          _tmpCreateTime = _stmt.getLong(_columnIndexOfCreateTime);
          _item.setCreateTime(_tmpCreateTime);
          final long _tmpOpening;
          _tmpOpening = _stmt.getLong(_columnIndexOfOpening);
          _item.setOpening(_tmpOpening);
          final long _tmpEnding;
          _tmpEnding = _stmt.getLong(_columnIndexOfEnding);
          _item.setEnding(_tmpEnding);
          final long _tmpPosition;
          _tmpPosition = _stmt.getLong(_columnIndexOfPosition);
          _item.setPosition(_tmpPosition);
          final long _tmpDuration;
          _tmpDuration = _stmt.getLong(_columnIndexOfDuration);
          _item.setDuration(_tmpDuration);
          final float _tmpSpeed;
          _tmpSpeed = (float) (_stmt.getDouble(_columnIndexOfSpeed));
          _item.setSpeed(_tmpSpeed);
          final int _tmpScale;
          _tmpScale = (int) (_stmt.getLong(_columnIndexOfScale));
          _item.setScale(_tmpScale);
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
  public List<History> findAll(final int cid) {
    final String _sql = "SELECT * FROM History WHERE cid = ?";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cid);
        final int _columnIndexOfKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "key");
        final int _columnIndexOfVodPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodPic");
        final int _columnIndexOfWallPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "wallPic");
        final int _columnIndexOfVodName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodName");
        final int _columnIndexOfVodFlag = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodFlag");
        final int _columnIndexOfVodRemarks = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodRemarks");
        final int _columnIndexOfEpisodeUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "episodeUrl");
        final int _columnIndexOfRevSort = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revSort");
        final int _columnIndexOfRevPlay = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revPlay");
        final int _columnIndexOfCreateTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createTime");
        final int _columnIndexOfOpening = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "opening");
        final int _columnIndexOfEnding = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "ending");
        final int _columnIndexOfPosition = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "position");
        final int _columnIndexOfDuration = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "duration");
        final int _columnIndexOfSpeed = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "speed");
        final int _columnIndexOfScale = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "scale");
        final int _columnIndexOfCid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "cid");
        final List<History> _result = new ArrayList<History>();
        while (_stmt.step()) {
          final History _item;
          _item = new History();
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfKey)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfKey);
          }
          _item.setKey(_tmpKey);
          final String _tmpVodPic;
          if (_stmt.isNull(_columnIndexOfVodPic)) {
            _tmpVodPic = null;
          } else {
            _tmpVodPic = _stmt.getText(_columnIndexOfVodPic);
          }
          _item.setVodPic(_tmpVodPic);
          final String _tmpWallPic;
          if (_stmt.isNull(_columnIndexOfWallPic)) {
            _tmpWallPic = null;
          } else {
            _tmpWallPic = _stmt.getText(_columnIndexOfWallPic);
          }
          _item.setWallPic(_tmpWallPic);
          final String _tmpVodName;
          if (_stmt.isNull(_columnIndexOfVodName)) {
            _tmpVodName = null;
          } else {
            _tmpVodName = _stmt.getText(_columnIndexOfVodName);
          }
          _item.setVodName(_tmpVodName);
          final String _tmpVodFlag;
          if (_stmt.isNull(_columnIndexOfVodFlag)) {
            _tmpVodFlag = null;
          } else {
            _tmpVodFlag = _stmt.getText(_columnIndexOfVodFlag);
          }
          _item.setVodFlag(_tmpVodFlag);
          final String _tmpVodRemarks;
          if (_stmt.isNull(_columnIndexOfVodRemarks)) {
            _tmpVodRemarks = null;
          } else {
            _tmpVodRemarks = _stmt.getText(_columnIndexOfVodRemarks);
          }
          _item.setVodRemarks(_tmpVodRemarks);
          final String _tmpEpisodeUrl;
          if (_stmt.isNull(_columnIndexOfEpisodeUrl)) {
            _tmpEpisodeUrl = null;
          } else {
            _tmpEpisodeUrl = _stmt.getText(_columnIndexOfEpisodeUrl);
          }
          _item.setEpisodeUrl(_tmpEpisodeUrl);
          final boolean _tmpRevSort;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfRevSort));
          _tmpRevSort = _tmp != 0;
          _item.setRevSort(_tmpRevSort);
          final boolean _tmpRevPlay;
          final int _tmp_1;
          _tmp_1 = (int) (_stmt.getLong(_columnIndexOfRevPlay));
          _tmpRevPlay = _tmp_1 != 0;
          _item.setRevPlay(_tmpRevPlay);
          final long _tmpCreateTime;
          _tmpCreateTime = _stmt.getLong(_columnIndexOfCreateTime);
          _item.setCreateTime(_tmpCreateTime);
          final long _tmpOpening;
          _tmpOpening = _stmt.getLong(_columnIndexOfOpening);
          _item.setOpening(_tmpOpening);
          final long _tmpEnding;
          _tmpEnding = _stmt.getLong(_columnIndexOfEnding);
          _item.setEnding(_tmpEnding);
          final long _tmpPosition;
          _tmpPosition = _stmt.getLong(_columnIndexOfPosition);
          _item.setPosition(_tmpPosition);
          final long _tmpDuration;
          _tmpDuration = _stmt.getLong(_columnIndexOfDuration);
          _item.setDuration(_tmpDuration);
          final float _tmpSpeed;
          _tmpSpeed = (float) (_stmt.getDouble(_columnIndexOfSpeed));
          _item.setSpeed(_tmpSpeed);
          final int _tmpScale;
          _tmpScale = (int) (_stmt.getLong(_columnIndexOfScale));
          _item.setScale(_tmpScale);
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
  public List<History> find(final int cid, final long createTime) {
    final String _sql = "SELECT * FROM History WHERE cid = ? AND createTime >= ? ORDER BY createTime DESC LIMIT 60";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cid);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, createTime);
        final int _columnIndexOfKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "key");
        final int _columnIndexOfVodPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodPic");
        final int _columnIndexOfWallPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "wallPic");
        final int _columnIndexOfVodName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodName");
        final int _columnIndexOfVodFlag = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodFlag");
        final int _columnIndexOfVodRemarks = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodRemarks");
        final int _columnIndexOfEpisodeUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "episodeUrl");
        final int _columnIndexOfRevSort = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revSort");
        final int _columnIndexOfRevPlay = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revPlay");
        final int _columnIndexOfCreateTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createTime");
        final int _columnIndexOfOpening = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "opening");
        final int _columnIndexOfEnding = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "ending");
        final int _columnIndexOfPosition = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "position");
        final int _columnIndexOfDuration = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "duration");
        final int _columnIndexOfSpeed = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "speed");
        final int _columnIndexOfScale = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "scale");
        final int _columnIndexOfCid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "cid");
        final List<History> _result = new ArrayList<History>();
        while (_stmt.step()) {
          final History _item;
          _item = new History();
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfKey)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfKey);
          }
          _item.setKey(_tmpKey);
          final String _tmpVodPic;
          if (_stmt.isNull(_columnIndexOfVodPic)) {
            _tmpVodPic = null;
          } else {
            _tmpVodPic = _stmt.getText(_columnIndexOfVodPic);
          }
          _item.setVodPic(_tmpVodPic);
          final String _tmpWallPic;
          if (_stmt.isNull(_columnIndexOfWallPic)) {
            _tmpWallPic = null;
          } else {
            _tmpWallPic = _stmt.getText(_columnIndexOfWallPic);
          }
          _item.setWallPic(_tmpWallPic);
          final String _tmpVodName;
          if (_stmt.isNull(_columnIndexOfVodName)) {
            _tmpVodName = null;
          } else {
            _tmpVodName = _stmt.getText(_columnIndexOfVodName);
          }
          _item.setVodName(_tmpVodName);
          final String _tmpVodFlag;
          if (_stmt.isNull(_columnIndexOfVodFlag)) {
            _tmpVodFlag = null;
          } else {
            _tmpVodFlag = _stmt.getText(_columnIndexOfVodFlag);
          }
          _item.setVodFlag(_tmpVodFlag);
          final String _tmpVodRemarks;
          if (_stmt.isNull(_columnIndexOfVodRemarks)) {
            _tmpVodRemarks = null;
          } else {
            _tmpVodRemarks = _stmt.getText(_columnIndexOfVodRemarks);
          }
          _item.setVodRemarks(_tmpVodRemarks);
          final String _tmpEpisodeUrl;
          if (_stmt.isNull(_columnIndexOfEpisodeUrl)) {
            _tmpEpisodeUrl = null;
          } else {
            _tmpEpisodeUrl = _stmt.getText(_columnIndexOfEpisodeUrl);
          }
          _item.setEpisodeUrl(_tmpEpisodeUrl);
          final boolean _tmpRevSort;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfRevSort));
          _tmpRevSort = _tmp != 0;
          _item.setRevSort(_tmpRevSort);
          final boolean _tmpRevPlay;
          final int _tmp_1;
          _tmp_1 = (int) (_stmt.getLong(_columnIndexOfRevPlay));
          _tmpRevPlay = _tmp_1 != 0;
          _item.setRevPlay(_tmpRevPlay);
          final long _tmpCreateTime;
          _tmpCreateTime = _stmt.getLong(_columnIndexOfCreateTime);
          _item.setCreateTime(_tmpCreateTime);
          final long _tmpOpening;
          _tmpOpening = _stmt.getLong(_columnIndexOfOpening);
          _item.setOpening(_tmpOpening);
          final long _tmpEnding;
          _tmpEnding = _stmt.getLong(_columnIndexOfEnding);
          _item.setEnding(_tmpEnding);
          final long _tmpPosition;
          _tmpPosition = _stmt.getLong(_columnIndexOfPosition);
          _item.setPosition(_tmpPosition);
          final long _tmpDuration;
          _tmpDuration = _stmt.getLong(_columnIndexOfDuration);
          _item.setDuration(_tmpDuration);
          final float _tmpSpeed;
          _tmpSpeed = (float) (_stmt.getDouble(_columnIndexOfSpeed));
          _item.setSpeed(_tmpSpeed);
          final int _tmpScale;
          _tmpScale = (int) (_stmt.getLong(_columnIndexOfScale));
          _item.setScale(_tmpScale);
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
  public History find(final int cid, final String key) {
    final String _sql = "SELECT * FROM History WHERE cid = ? AND `key` = ?";
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
        final int _columnIndexOfVodPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodPic");
        final int _columnIndexOfWallPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "wallPic");
        final int _columnIndexOfVodName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodName");
        final int _columnIndexOfVodFlag = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodFlag");
        final int _columnIndexOfVodRemarks = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodRemarks");
        final int _columnIndexOfEpisodeUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "episodeUrl");
        final int _columnIndexOfRevSort = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revSort");
        final int _columnIndexOfRevPlay = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revPlay");
        final int _columnIndexOfCreateTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createTime");
        final int _columnIndexOfOpening = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "opening");
        final int _columnIndexOfEnding = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "ending");
        final int _columnIndexOfPosition = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "position");
        final int _columnIndexOfDuration = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "duration");
        final int _columnIndexOfSpeed = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "speed");
        final int _columnIndexOfScale = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "scale");
        final int _columnIndexOfCid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "cid");
        final History _result;
        if (_stmt.step()) {
          _result = new History();
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfKey)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfKey);
          }
          _result.setKey(_tmpKey);
          final String _tmpVodPic;
          if (_stmt.isNull(_columnIndexOfVodPic)) {
            _tmpVodPic = null;
          } else {
            _tmpVodPic = _stmt.getText(_columnIndexOfVodPic);
          }
          _result.setVodPic(_tmpVodPic);
          final String _tmpWallPic;
          if (_stmt.isNull(_columnIndexOfWallPic)) {
            _tmpWallPic = null;
          } else {
            _tmpWallPic = _stmt.getText(_columnIndexOfWallPic);
          }
          _result.setWallPic(_tmpWallPic);
          final String _tmpVodName;
          if (_stmt.isNull(_columnIndexOfVodName)) {
            _tmpVodName = null;
          } else {
            _tmpVodName = _stmt.getText(_columnIndexOfVodName);
          }
          _result.setVodName(_tmpVodName);
          final String _tmpVodFlag;
          if (_stmt.isNull(_columnIndexOfVodFlag)) {
            _tmpVodFlag = null;
          } else {
            _tmpVodFlag = _stmt.getText(_columnIndexOfVodFlag);
          }
          _result.setVodFlag(_tmpVodFlag);
          final String _tmpVodRemarks;
          if (_stmt.isNull(_columnIndexOfVodRemarks)) {
            _tmpVodRemarks = null;
          } else {
            _tmpVodRemarks = _stmt.getText(_columnIndexOfVodRemarks);
          }
          _result.setVodRemarks(_tmpVodRemarks);
          final String _tmpEpisodeUrl;
          if (_stmt.isNull(_columnIndexOfEpisodeUrl)) {
            _tmpEpisodeUrl = null;
          } else {
            _tmpEpisodeUrl = _stmt.getText(_columnIndexOfEpisodeUrl);
          }
          _result.setEpisodeUrl(_tmpEpisodeUrl);
          final boolean _tmpRevSort;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfRevSort));
          _tmpRevSort = _tmp != 0;
          _result.setRevSort(_tmpRevSort);
          final boolean _tmpRevPlay;
          final int _tmp_1;
          _tmp_1 = (int) (_stmt.getLong(_columnIndexOfRevPlay));
          _tmpRevPlay = _tmp_1 != 0;
          _result.setRevPlay(_tmpRevPlay);
          final long _tmpCreateTime;
          _tmpCreateTime = _stmt.getLong(_columnIndexOfCreateTime);
          _result.setCreateTime(_tmpCreateTime);
          final long _tmpOpening;
          _tmpOpening = _stmt.getLong(_columnIndexOfOpening);
          _result.setOpening(_tmpOpening);
          final long _tmpEnding;
          _tmpEnding = _stmt.getLong(_columnIndexOfEnding);
          _result.setEnding(_tmpEnding);
          final long _tmpPosition;
          _tmpPosition = _stmt.getLong(_columnIndexOfPosition);
          _result.setPosition(_tmpPosition);
          final long _tmpDuration;
          _tmpDuration = _stmt.getLong(_columnIndexOfDuration);
          _result.setDuration(_tmpDuration);
          final float _tmpSpeed;
          _tmpSpeed = (float) (_stmt.getDouble(_columnIndexOfSpeed));
          _result.setSpeed(_tmpSpeed);
          final int _tmpScale;
          _tmpScale = (int) (_stmt.getLong(_columnIndexOfScale));
          _result.setScale(_tmpScale);
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
  public List<History> findByName(final int cid, final String vodName) {
    final String _sql = "SELECT * FROM History WHERE cid = ? AND vodName = ? ORDER BY createTime DESC";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cid);
        _argIndex = 2;
        if (vodName == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, vodName);
        }
        final int _columnIndexOfKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "key");
        final int _columnIndexOfVodPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodPic");
        final int _columnIndexOfWallPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "wallPic");
        final int _columnIndexOfVodName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodName");
        final int _columnIndexOfVodFlag = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodFlag");
        final int _columnIndexOfVodRemarks = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodRemarks");
        final int _columnIndexOfEpisodeUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "episodeUrl");
        final int _columnIndexOfRevSort = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revSort");
        final int _columnIndexOfRevPlay = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revPlay");
        final int _columnIndexOfCreateTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createTime");
        final int _columnIndexOfOpening = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "opening");
        final int _columnIndexOfEnding = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "ending");
        final int _columnIndexOfPosition = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "position");
        final int _columnIndexOfDuration = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "duration");
        final int _columnIndexOfSpeed = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "speed");
        final int _columnIndexOfScale = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "scale");
        final int _columnIndexOfCid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "cid");
        final List<History> _result = new ArrayList<History>();
        while (_stmt.step()) {
          final History _item;
          _item = new History();
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfKey)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfKey);
          }
          _item.setKey(_tmpKey);
          final String _tmpVodPic;
          if (_stmt.isNull(_columnIndexOfVodPic)) {
            _tmpVodPic = null;
          } else {
            _tmpVodPic = _stmt.getText(_columnIndexOfVodPic);
          }
          _item.setVodPic(_tmpVodPic);
          final String _tmpWallPic;
          if (_stmt.isNull(_columnIndexOfWallPic)) {
            _tmpWallPic = null;
          } else {
            _tmpWallPic = _stmt.getText(_columnIndexOfWallPic);
          }
          _item.setWallPic(_tmpWallPic);
          final String _tmpVodName;
          if (_stmt.isNull(_columnIndexOfVodName)) {
            _tmpVodName = null;
          } else {
            _tmpVodName = _stmt.getText(_columnIndexOfVodName);
          }
          _item.setVodName(_tmpVodName);
          final String _tmpVodFlag;
          if (_stmt.isNull(_columnIndexOfVodFlag)) {
            _tmpVodFlag = null;
          } else {
            _tmpVodFlag = _stmt.getText(_columnIndexOfVodFlag);
          }
          _item.setVodFlag(_tmpVodFlag);
          final String _tmpVodRemarks;
          if (_stmt.isNull(_columnIndexOfVodRemarks)) {
            _tmpVodRemarks = null;
          } else {
            _tmpVodRemarks = _stmt.getText(_columnIndexOfVodRemarks);
          }
          _item.setVodRemarks(_tmpVodRemarks);
          final String _tmpEpisodeUrl;
          if (_stmt.isNull(_columnIndexOfEpisodeUrl)) {
            _tmpEpisodeUrl = null;
          } else {
            _tmpEpisodeUrl = _stmt.getText(_columnIndexOfEpisodeUrl);
          }
          _item.setEpisodeUrl(_tmpEpisodeUrl);
          final boolean _tmpRevSort;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfRevSort));
          _tmpRevSort = _tmp != 0;
          _item.setRevSort(_tmpRevSort);
          final boolean _tmpRevPlay;
          final int _tmp_1;
          _tmp_1 = (int) (_stmt.getLong(_columnIndexOfRevPlay));
          _tmpRevPlay = _tmp_1 != 0;
          _item.setRevPlay(_tmpRevPlay);
          final long _tmpCreateTime;
          _tmpCreateTime = _stmt.getLong(_columnIndexOfCreateTime);
          _item.setCreateTime(_tmpCreateTime);
          final long _tmpOpening;
          _tmpOpening = _stmt.getLong(_columnIndexOfOpening);
          _item.setOpening(_tmpOpening);
          final long _tmpEnding;
          _tmpEnding = _stmt.getLong(_columnIndexOfEnding);
          _item.setEnding(_tmpEnding);
          final long _tmpPosition;
          _tmpPosition = _stmt.getLong(_columnIndexOfPosition);
          _item.setPosition(_tmpPosition);
          final long _tmpDuration;
          _tmpDuration = _stmt.getLong(_columnIndexOfDuration);
          _item.setDuration(_tmpDuration);
          final float _tmpSpeed;
          _tmpSpeed = (float) (_stmt.getDouble(_columnIndexOfSpeed));
          _item.setSpeed(_tmpSpeed);
          final int _tmpScale;
          _tmpScale = (int) (_stmt.getLong(_columnIndexOfScale));
          _item.setScale(_tmpScale);
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
  public List<History> findByKeyPrefix(final int cid, final String keyPrefix) {
    final String _sql = "SELECT * FROM History WHERE cid = ? AND `key` LIKE ? || '%' ORDER BY createTime DESC";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cid);
        _argIndex = 2;
        if (keyPrefix == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, keyPrefix);
        }
        final int _columnIndexOfKey = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "key");
        final int _columnIndexOfVodPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodPic");
        final int _columnIndexOfWallPic = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "wallPic");
        final int _columnIndexOfVodName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodName");
        final int _columnIndexOfVodFlag = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodFlag");
        final int _columnIndexOfVodRemarks = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "vodRemarks");
        final int _columnIndexOfEpisodeUrl = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "episodeUrl");
        final int _columnIndexOfRevSort = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revSort");
        final int _columnIndexOfRevPlay = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "revPlay");
        final int _columnIndexOfCreateTime = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "createTime");
        final int _columnIndexOfOpening = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "opening");
        final int _columnIndexOfEnding = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "ending");
        final int _columnIndexOfPosition = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "position");
        final int _columnIndexOfDuration = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "duration");
        final int _columnIndexOfSpeed = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "speed");
        final int _columnIndexOfScale = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "scale");
        final int _columnIndexOfCid = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "cid");
        final List<History> _result = new ArrayList<History>();
        while (_stmt.step()) {
          final History _item;
          _item = new History();
          final String _tmpKey;
          if (_stmt.isNull(_columnIndexOfKey)) {
            _tmpKey = null;
          } else {
            _tmpKey = _stmt.getText(_columnIndexOfKey);
          }
          _item.setKey(_tmpKey);
          final String _tmpVodPic;
          if (_stmt.isNull(_columnIndexOfVodPic)) {
            _tmpVodPic = null;
          } else {
            _tmpVodPic = _stmt.getText(_columnIndexOfVodPic);
          }
          _item.setVodPic(_tmpVodPic);
          final String _tmpWallPic;
          if (_stmt.isNull(_columnIndexOfWallPic)) {
            _tmpWallPic = null;
          } else {
            _tmpWallPic = _stmt.getText(_columnIndexOfWallPic);
          }
          _item.setWallPic(_tmpWallPic);
          final String _tmpVodName;
          if (_stmt.isNull(_columnIndexOfVodName)) {
            _tmpVodName = null;
          } else {
            _tmpVodName = _stmt.getText(_columnIndexOfVodName);
          }
          _item.setVodName(_tmpVodName);
          final String _tmpVodFlag;
          if (_stmt.isNull(_columnIndexOfVodFlag)) {
            _tmpVodFlag = null;
          } else {
            _tmpVodFlag = _stmt.getText(_columnIndexOfVodFlag);
          }
          _item.setVodFlag(_tmpVodFlag);
          final String _tmpVodRemarks;
          if (_stmt.isNull(_columnIndexOfVodRemarks)) {
            _tmpVodRemarks = null;
          } else {
            _tmpVodRemarks = _stmt.getText(_columnIndexOfVodRemarks);
          }
          _item.setVodRemarks(_tmpVodRemarks);
          final String _tmpEpisodeUrl;
          if (_stmt.isNull(_columnIndexOfEpisodeUrl)) {
            _tmpEpisodeUrl = null;
          } else {
            _tmpEpisodeUrl = _stmt.getText(_columnIndexOfEpisodeUrl);
          }
          _item.setEpisodeUrl(_tmpEpisodeUrl);
          final boolean _tmpRevSort;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfRevSort));
          _tmpRevSort = _tmp != 0;
          _item.setRevSort(_tmpRevSort);
          final boolean _tmpRevPlay;
          final int _tmp_1;
          _tmp_1 = (int) (_stmt.getLong(_columnIndexOfRevPlay));
          _tmpRevPlay = _tmp_1 != 0;
          _item.setRevPlay(_tmpRevPlay);
          final long _tmpCreateTime;
          _tmpCreateTime = _stmt.getLong(_columnIndexOfCreateTime);
          _item.setCreateTime(_tmpCreateTime);
          final long _tmpOpening;
          _tmpOpening = _stmt.getLong(_columnIndexOfOpening);
          _item.setOpening(_tmpOpening);
          final long _tmpEnding;
          _tmpEnding = _stmt.getLong(_columnIndexOfEnding);
          _item.setEnding(_tmpEnding);
          final long _tmpPosition;
          _tmpPosition = _stmt.getLong(_columnIndexOfPosition);
          _item.setPosition(_tmpPosition);
          final long _tmpDuration;
          _tmpDuration = _stmt.getLong(_columnIndexOfDuration);
          _item.setDuration(_tmpDuration);
          final float _tmpSpeed;
          _tmpSpeed = (float) (_stmt.getDouble(_columnIndexOfSpeed));
          _item.setSpeed(_tmpSpeed);
          final int _tmpScale;
          _tmpScale = (int) (_stmt.getLong(_columnIndexOfScale));
          _item.setScale(_tmpScale);
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
  public int delete(final int cid, final String key) {
    final String _sql = "DELETE FROM History WHERE cid = ? AND `key` = ?";
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
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
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int delete(final int cid) {
    final String _sql = "DELETE FROM History WHERE cid = ?";
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cid);
        _stmt.step();
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int delete() {
    final String _sql = "DELETE FROM History";
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        _stmt.step();
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int deleteByKeyPrefix(final int cid, final String keyPrefix) {
    final String _sql = "DELETE FROM History WHERE cid = ? AND `key` LIKE ? || '%'";
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cid);
        _argIndex = 2;
        if (keyPrefix == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, keyPrefix);
        }
        _stmt.step();
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
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
