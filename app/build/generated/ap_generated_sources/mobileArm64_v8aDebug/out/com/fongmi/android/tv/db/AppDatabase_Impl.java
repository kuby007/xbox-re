package com.fongmi.android.tv.db;

import androidx.annotation.NonNull;
import androidx.room.InvalidationTracker;
import androidx.room.RoomOpenDelegate;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.SQLite;
import androidx.sqlite.SQLiteConnection;
import com.fongmi.android.tv.db.dao.ConfigDao;
import com.fongmi.android.tv.db.dao.ConfigDao_Impl;
import com.fongmi.android.tv.db.dao.DeviceDao;
import com.fongmi.android.tv.db.dao.DeviceDao_Impl;
import com.fongmi.android.tv.db.dao.HistoryDao;
import com.fongmi.android.tv.db.dao.HistoryDao_Impl;
import com.fongmi.android.tv.db.dao.KeepDao;
import com.fongmi.android.tv.db.dao.KeepDao_Impl;
import com.fongmi.android.tv.db.dao.LiveDao;
import com.fongmi.android.tv.db.dao.LiveDao_Impl;
import com.fongmi.android.tv.db.dao.PlaybackDeleteTombstoneDao;
import com.fongmi.android.tv.db.dao.PlaybackDeleteTombstoneDao_Impl;
import com.fongmi.android.tv.db.dao.SiteDao;
import com.fongmi.android.tv.db.dao.SiteDao_Impl;
import com.fongmi.android.tv.db.dao.TrackDao;
import com.fongmi.android.tv.db.dao.TrackDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile KeepDao _keepDao;

  private volatile SiteDao _siteDao;

  private volatile LiveDao _liveDao;

  private volatile TrackDao _trackDao;

  private volatile ConfigDao _configDao;

  private volatile DeviceDao _deviceDao;

  private volatile HistoryDao _historyDao;

  private volatile PlaybackDeleteTombstoneDao _playbackDeleteTombstoneDao;

  @Override
  @NonNull
  protected RoomOpenDelegate createOpenDelegate() {
    final RoomOpenDelegate _openDelegate = new RoomOpenDelegate(37, "b1761c4b8b9eabf737c80884e6086d11", "3ebac4c0d5ab0a5a30582962aac0108e") {
      @Override
      public void createAllTables(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `Keep` (`key` TEXT NOT NULL, `siteName` TEXT, `vodName` TEXT, `vodPic` TEXT, `createTime` INTEGER NOT NULL, `type` INTEGER NOT NULL, `cid` INTEGER NOT NULL, PRIMARY KEY(`key`))");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `Site` (`key` TEXT NOT NULL, `searchable` INTEGER, `changeable` INTEGER, PRIMARY KEY(`key`))");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `Live` (`name` TEXT NOT NULL, `keep` TEXT, `boot` INTEGER NOT NULL, `pass` INTEGER NOT NULL, PRIMARY KEY(`name`))");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `Track` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` INTEGER NOT NULL, `key` TEXT, `name` TEXT, `format` TEXT, `selected` INTEGER NOT NULL)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_Track_key_type` ON `Track` (`key`, `type`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `Config` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` INTEGER NOT NULL, `time` INTEGER NOT NULL, `url` TEXT, `json` TEXT, `name` TEXT, `logo` TEXT, `home` TEXT, `parse` TEXT)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_Config_url_type` ON `Config` (`url`, `type`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `Device` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `uuid` TEXT, `name` TEXT, `ip` TEXT, `type` INTEGER NOT NULL)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_Device_uuid_name` ON `Device` (`uuid`, `name`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `History` (`key` TEXT NOT NULL, `vodPic` TEXT, `wallPic` TEXT, `vodName` TEXT, `vodFlag` TEXT, `vodRemarks` TEXT, `episodeUrl` TEXT, `revSort` INTEGER NOT NULL, `revPlay` INTEGER NOT NULL, `createTime` INTEGER NOT NULL, `opening` INTEGER NOT NULL, `ending` INTEGER NOT NULL, `position` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `speed` REAL NOT NULL, `scale` INTEGER NOT NULL, `cid` INTEGER NOT NULL, PRIMARY KEY(`key`))");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `PlaybackDeleteTombstone` (`id` TEXT NOT NULL, `configKey` TEXT NOT NULL, `scope` TEXT NOT NULL, `historyKey` TEXT NOT NULL, `siteKey` TEXT NOT NULL, `vodId` TEXT NOT NULL, `deletedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_PlaybackDeleteTombstone_deletedAt` ON `PlaybackDeleteTombstone` (`deletedAt`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        SQLite.execSQL(connection, "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b1761c4b8b9eabf737c80884e6086d11')");
      }

      @Override
      public void dropAllTables(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `Keep`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `Site`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `Live`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `Track`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `Config`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `Device`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `History`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `PlaybackDeleteTombstone`");
      }

      @Override
      public void onCreate(@NonNull final SQLiteConnection connection) {
      }

      @Override
      public void onOpen(@NonNull final SQLiteConnection connection) {
        internalInitInvalidationTracker(connection);
      }

      @Override
      public void onPreMigrate(@NonNull final SQLiteConnection connection) {
        DBUtil.dropFtsSyncTriggers(connection);
      }

      @Override
      public void onPostMigrate(@NonNull final SQLiteConnection connection) {
      }

      @Override
      @NonNull
      public RoomOpenDelegate.ValidationResult onValidateSchema(
          @NonNull final SQLiteConnection connection) {
        final Map<String, TableInfo.Column> _columnsKeep = new HashMap<String, TableInfo.Column>(7);
        _columnsKeep.put("key", new TableInfo.Column("key", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKeep.put("siteName", new TableInfo.Column("siteName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKeep.put("vodName", new TableInfo.Column("vodName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKeep.put("vodPic", new TableInfo.Column("vodPic", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKeep.put("createTime", new TableInfo.Column("createTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKeep.put("type", new TableInfo.Column("type", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKeep.put("cid", new TableInfo.Column("cid", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysKeep = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesKeep = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoKeep = new TableInfo("Keep", _columnsKeep, _foreignKeysKeep, _indicesKeep);
        final TableInfo _existingKeep = TableInfo.read(connection, "Keep");
        if (!_infoKeep.equals(_existingKeep)) {
          return new RoomOpenDelegate.ValidationResult(false, "Keep(com.fongmi.android.tv.bean.Keep).\n"
                  + " Expected:\n" + _infoKeep + "\n"
                  + " Found:\n" + _existingKeep);
        }
        final Map<String, TableInfo.Column> _columnsSite = new HashMap<String, TableInfo.Column>(3);
        _columnsSite.put("key", new TableInfo.Column("key", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSite.put("searchable", new TableInfo.Column("searchable", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSite.put("changeable", new TableInfo.Column("changeable", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysSite = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesSite = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSite = new TableInfo("Site", _columnsSite, _foreignKeysSite, _indicesSite);
        final TableInfo _existingSite = TableInfo.read(connection, "Site");
        if (!_infoSite.equals(_existingSite)) {
          return new RoomOpenDelegate.ValidationResult(false, "Site(com.fongmi.android.tv.bean.Site).\n"
                  + " Expected:\n" + _infoSite + "\n"
                  + " Found:\n" + _existingSite);
        }
        final Map<String, TableInfo.Column> _columnsLive = new HashMap<String, TableInfo.Column>(4);
        _columnsLive.put("name", new TableInfo.Column("name", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLive.put("keep", new TableInfo.Column("keep", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLive.put("boot", new TableInfo.Column("boot", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLive.put("pass", new TableInfo.Column("pass", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysLive = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesLive = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLive = new TableInfo("Live", _columnsLive, _foreignKeysLive, _indicesLive);
        final TableInfo _existingLive = TableInfo.read(connection, "Live");
        if (!_infoLive.equals(_existingLive)) {
          return new RoomOpenDelegate.ValidationResult(false, "Live(com.fongmi.android.tv.bean.Live).\n"
                  + " Expected:\n" + _infoLive + "\n"
                  + " Found:\n" + _existingLive);
        }
        final Map<String, TableInfo.Column> _columnsTrack = new HashMap<String, TableInfo.Column>(6);
        _columnsTrack.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrack.put("type", new TableInfo.Column("type", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrack.put("key", new TableInfo.Column("key", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrack.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrack.put("format", new TableInfo.Column("format", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrack.put("selected", new TableInfo.Column("selected", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysTrack = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesTrack = new HashSet<TableInfo.Index>(1);
        _indicesTrack.add(new TableInfo.Index("index_Track_key_type", true, Arrays.asList("key", "type"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoTrack = new TableInfo("Track", _columnsTrack, _foreignKeysTrack, _indicesTrack);
        final TableInfo _existingTrack = TableInfo.read(connection, "Track");
        if (!_infoTrack.equals(_existingTrack)) {
          return new RoomOpenDelegate.ValidationResult(false, "Track(com.fongmi.android.tv.bean.Track).\n"
                  + " Expected:\n" + _infoTrack + "\n"
                  + " Found:\n" + _existingTrack);
        }
        final Map<String, TableInfo.Column> _columnsConfig = new HashMap<String, TableInfo.Column>(9);
        _columnsConfig.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConfig.put("type", new TableInfo.Column("type", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConfig.put("time", new TableInfo.Column("time", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConfig.put("url", new TableInfo.Column("url", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConfig.put("json", new TableInfo.Column("json", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConfig.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConfig.put("logo", new TableInfo.Column("logo", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConfig.put("home", new TableInfo.Column("home", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConfig.put("parse", new TableInfo.Column("parse", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysConfig = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesConfig = new HashSet<TableInfo.Index>(1);
        _indicesConfig.add(new TableInfo.Index("index_Config_url_type", true, Arrays.asList("url", "type"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoConfig = new TableInfo("Config", _columnsConfig, _foreignKeysConfig, _indicesConfig);
        final TableInfo _existingConfig = TableInfo.read(connection, "Config");
        if (!_infoConfig.equals(_existingConfig)) {
          return new RoomOpenDelegate.ValidationResult(false, "Config(com.fongmi.android.tv.bean.Config).\n"
                  + " Expected:\n" + _infoConfig + "\n"
                  + " Found:\n" + _existingConfig);
        }
        final Map<String, TableInfo.Column> _columnsDevice = new HashMap<String, TableInfo.Column>(5);
        _columnsDevice.put("id", new TableInfo.Column("id", "INTEGER", false, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevice.put("uuid", new TableInfo.Column("uuid", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevice.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevice.put("ip", new TableInfo.Column("ip", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDevice.put("type", new TableInfo.Column("type", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysDevice = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesDevice = new HashSet<TableInfo.Index>(1);
        _indicesDevice.add(new TableInfo.Index("index_Device_uuid_name", true, Arrays.asList("uuid", "name"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoDevice = new TableInfo("Device", _columnsDevice, _foreignKeysDevice, _indicesDevice);
        final TableInfo _existingDevice = TableInfo.read(connection, "Device");
        if (!_infoDevice.equals(_existingDevice)) {
          return new RoomOpenDelegate.ValidationResult(false, "Device(com.fongmi.android.tv.bean.Device).\n"
                  + " Expected:\n" + _infoDevice + "\n"
                  + " Found:\n" + _existingDevice);
        }
        final Map<String, TableInfo.Column> _columnsHistory = new HashMap<String, TableInfo.Column>(17);
        _columnsHistory.put("key", new TableInfo.Column("key", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("vodPic", new TableInfo.Column("vodPic", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("wallPic", new TableInfo.Column("wallPic", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("vodName", new TableInfo.Column("vodName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("vodFlag", new TableInfo.Column("vodFlag", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("vodRemarks", new TableInfo.Column("vodRemarks", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("episodeUrl", new TableInfo.Column("episodeUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("revSort", new TableInfo.Column("revSort", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("revPlay", new TableInfo.Column("revPlay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("createTime", new TableInfo.Column("createTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("opening", new TableInfo.Column("opening", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("ending", new TableInfo.Column("ending", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("position", new TableInfo.Column("position", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("speed", new TableInfo.Column("speed", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("scale", new TableInfo.Column("scale", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("cid", new TableInfo.Column("cid", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysHistory = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHistory = new TableInfo("History", _columnsHistory, _foreignKeysHistory, _indicesHistory);
        final TableInfo _existingHistory = TableInfo.read(connection, "History");
        if (!_infoHistory.equals(_existingHistory)) {
          return new RoomOpenDelegate.ValidationResult(false, "History(com.fongmi.android.tv.bean.History).\n"
                  + " Expected:\n" + _infoHistory + "\n"
                  + " Found:\n" + _existingHistory);
        }
        final Map<String, TableInfo.Column> _columnsPlaybackDeleteTombstone = new HashMap<String, TableInfo.Column>(7);
        _columnsPlaybackDeleteTombstone.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackDeleteTombstone.put("configKey", new TableInfo.Column("configKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackDeleteTombstone.put("scope", new TableInfo.Column("scope", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackDeleteTombstone.put("historyKey", new TableInfo.Column("historyKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackDeleteTombstone.put("siteKey", new TableInfo.Column("siteKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackDeleteTombstone.put("vodId", new TableInfo.Column("vodId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackDeleteTombstone.put("deletedAt", new TableInfo.Column("deletedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysPlaybackDeleteTombstone = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesPlaybackDeleteTombstone = new HashSet<TableInfo.Index>(1);
        _indicesPlaybackDeleteTombstone.add(new TableInfo.Index("index_PlaybackDeleteTombstone_deletedAt", false, Arrays.asList("deletedAt"), Arrays.asList("ASC")));
        final TableInfo _infoPlaybackDeleteTombstone = new TableInfo("PlaybackDeleteTombstone", _columnsPlaybackDeleteTombstone, _foreignKeysPlaybackDeleteTombstone, _indicesPlaybackDeleteTombstone);
        final TableInfo _existingPlaybackDeleteTombstone = TableInfo.read(connection, "PlaybackDeleteTombstone");
        if (!_infoPlaybackDeleteTombstone.equals(_existingPlaybackDeleteTombstone)) {
          return new RoomOpenDelegate.ValidationResult(false, "PlaybackDeleteTombstone(com.fongmi.android.tv.bean.PlaybackDeleteTombstone).\n"
                  + " Expected:\n" + _infoPlaybackDeleteTombstone + "\n"
                  + " Found:\n" + _existingPlaybackDeleteTombstone);
        }
        return new RoomOpenDelegate.ValidationResult(true, null);
      }
    };
    return _openDelegate;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final Map<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final Map<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "Keep", "Site", "Live", "Track", "Config", "Device", "History", "PlaybackDeleteTombstone");
  }

  @Override
  public void clearAllTables() {
    super.performClear(false, "Keep", "Site", "Live", "Track", "Config", "Device", "History", "PlaybackDeleteTombstone");
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final Map<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(KeepDao.class, KeepDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SiteDao.class, SiteDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LiveDao.class, LiveDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TrackDao.class, TrackDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ConfigDao.class, ConfigDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DeviceDao.class, DeviceDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HistoryDao.class, HistoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PlaybackDeleteTombstoneDao.class, PlaybackDeleteTombstoneDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final Set<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public KeepDao getKeepDao() {
    if (_keepDao != null) {
      return _keepDao;
    } else {
      synchronized(this) {
        if(_keepDao == null) {
          _keepDao = new KeepDao_Impl(this);
        }
        return _keepDao;
      }
    }
  }

  @Override
  public SiteDao getSiteDao() {
    if (_siteDao != null) {
      return _siteDao;
    } else {
      synchronized(this) {
        if(_siteDao == null) {
          _siteDao = new SiteDao_Impl(this);
        }
        return _siteDao;
      }
    }
  }

  @Override
  public LiveDao getLiveDao() {
    if (_liveDao != null) {
      return _liveDao;
    } else {
      synchronized(this) {
        if(_liveDao == null) {
          _liveDao = new LiveDao_Impl(this);
        }
        return _liveDao;
      }
    }
  }

  @Override
  public TrackDao getTrackDao() {
    if (_trackDao != null) {
      return _trackDao;
    } else {
      synchronized(this) {
        if(_trackDao == null) {
          _trackDao = new TrackDao_Impl(this);
        }
        return _trackDao;
      }
    }
  }

  @Override
  public ConfigDao getConfigDao() {
    if (_configDao != null) {
      return _configDao;
    } else {
      synchronized(this) {
        if(_configDao == null) {
          _configDao = new ConfigDao_Impl(this);
        }
        return _configDao;
      }
    }
  }

  @Override
  public DeviceDao getDeviceDao() {
    if (_deviceDao != null) {
      return _deviceDao;
    } else {
      synchronized(this) {
        if(_deviceDao == null) {
          _deviceDao = new DeviceDao_Impl(this);
        }
        return _deviceDao;
      }
    }
  }

  @Override
  public HistoryDao getHistoryDao() {
    if (_historyDao != null) {
      return _historyDao;
    } else {
      synchronized(this) {
        if(_historyDao == null) {
          _historyDao = new HistoryDao_Impl(this);
        }
        return _historyDao;
      }
    }
  }

  @Override
  public PlaybackDeleteTombstoneDao getPlaybackDeleteTombstoneDao() {
    if (_playbackDeleteTombstoneDao != null) {
      return _playbackDeleteTombstoneDao;
    } else {
      synchronized(this) {
        if(_playbackDeleteTombstoneDao == null) {
          _playbackDeleteTombstoneDao = new PlaybackDeleteTombstoneDao_Impl(this);
        }
        return _playbackDeleteTombstoneDao;
      }
    }
  }
}
