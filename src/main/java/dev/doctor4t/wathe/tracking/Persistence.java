package dev.doctor4t.wathe.tracking;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// todo get better name
public abstract class Persistence {
    /**
     * Get the SQL server DB connection.
     * @return SQL server DB connection.
     */
    public abstract Connection getConnection();

    /**
     * Adds a column to the DB if it doesn't exist.
     *
     * Based on https://github.com/Athlaeos/ValhallaMMO/blob/master/core/src/main/java/me/athlaeos/valhallammo/persistence/implementations/SQL.java#L57
     * @param tableName Table name
     * @param columnName Column name
     * @param columnType Column type
     * @apiNote table name, column name and column type are vulnerable to SQL injections, so please don't screw it up
     */
    public void addColumnIfNotExists(String tableName, String columnName, String columnType) {
        Connection conn = getConnection();
        try (PreparedStatement verifyExistsStatement = conn.prepareStatement("SELECT %s FROM %s LIMIT 1;".formatted(columnName, tableName))) {
            verifyExistsStatement.execute();
        } catch (SQLException ignored){
            Wathe.LOGGER.info("Column {} does not exist in {}; creating with type {}", columnName, tableName, columnType);
            try (PreparedStatement creationStatement = conn
                    .prepareStatement("ALTER TABLE %s ADD COLUMN %s %s;".formatted(tableName, columnName, columnType))) {
                creationStatement.execute();
            } catch (SQLException ex){
                Wathe.LOGGER.error("SQLException when trying to add column {} {} to {}. ", columnName, columnType, tableName, ex);
            }
        }
    }

    public void initializeTables() {
        initializeGamesTable();
        initializePlayedInTable();
        initializePlayerStatsView();
    }

    public void persistFinishedGame(ServerWorld serverWorld, GameFunctions.WinStatus winStatus) {
        initializeTables();
        List<ServerPlayerEntity> players = serverWorld.getPlayers();
        UUID gameUuid = UUID.randomUUID(); // todo maybe not put this here?
        String winners = winStatus == GameFunctions.WinStatus.KILLERS ? winStatus.toString() : GameFunctions.WinStatus.PASSENGERS.toString();
        persistIntoGamesTable(gameUuid, winners, winStatus);
        GameWorldComponent game = GameWorldComponent.KEY.get(serverWorld);
        for (ServerPlayerEntity player : players) {
            String role;
            if (game.canUseKillerFeatures(player)) {
                role = "KILLER";
            } else if (game.isRole(player, WatheRoles.VIGILANTE)) {
                role = "VIGILANTE";
            } else {
                role = "CIVILIAN";
            }
            boolean alive = GameFunctions.isPlayerAliveAndSurvival(player);
            boolean left = false; // TODO figure out calculation
            persistIntoPlayedInTable(gameUuid, player.getUuid(), role, !alive, left);
        }
    }

    protected void persistIntoGamesTable(UUID gameUuid, String winners, GameFunctions.WinStatus winStatus) {
        String statement = "INSERT INTO Games VALUES (?, ?, ?, ?)";
        Connection conn = getConnection();
        Instant instant = Instant.now();
        try (PreparedStatement insertStatement = conn.prepareStatement(statement)) {
            insertStatement.setString(1, gameUuid.toString());
            insertStatement.setString(2, winners);
            insertStatement.setString(3, winStatus.toString());
            insertStatement.setLong(4, instant.getEpochSecond());
            insertStatement.execute();
        } catch (SQLException ex) {
            Wathe.LOGGER.error("SQLException when trying to insert into Games table", ex);
        }
    }

    protected void persistIntoPlayedInTable(UUID gameUuid, UUID playerUuid, String role, boolean dead, boolean left) {
        String statement = "INSERT INTO PlayedIn VALUES (?, ?, ?, ?, ?)";
        Connection conn = getConnection();
        try (PreparedStatement insertStatement = conn.prepareStatement(statement)) {
            insertStatement.setString(1, playerUuid.toString());
            insertStatement.setString(2, gameUuid.toString());
            insertStatement.setString(3, role);
            insertStatement.setInt(4, dead ? 1 : 0);
            insertStatement.setInt(5, left ? 1 : 0);
            insertStatement.execute();
        } catch (SQLException ex) {
            Wathe.LOGGER.error("SQLException when trying to insert into PlayedIn table", ex);
        }
    }

    protected void initializePlayerStatsView() {
        // thanks GPT very cool
        String sqlStatement = """
                CREATE VIEW PlayerStats AS
                SELECT *,
                       (games_won * 1.0 / games_played) AS winrate
                FROM (
                    SELECT
                        p.playerid,
                        COUNT(*) AS games_played,
                
                        SUM(
                            CASE
                                WHEN g.winners = 'KILLERS' AND p.rolename = 'KILLER' THEN 1
                                WHEN g.winners = 'PASSENGERS' AND p.rolename IN ('CIVILIAN', 'VIGILANTE') THEN 1
                                ELSE 0
                            END
                        ) AS games_won,
                
                        SUM(
                            CASE
                                WHEN g.winners = 'PASSENGERS' AND p.rolename = 'CIVILIAN' THEN 1
                                ELSE 0
                            END
                        ) AS civilian_wins,
                
                        SUM(
                            CASE
                                WHEN g.winners = 'KILLERS' AND p.rolename = 'KILLER' THEN 1
                                ELSE 0
                            END
                        ) AS killer_wins,
                
                        SUM(
                            CASE
                                WHEN g.winners = 'PASSENGERS' AND p.rolename = 'VIGILANTE' THEN 1
                                ELSE 0
                            END
                        ) AS vigilante_wins
                
                    FROM PlayedIn p
                    JOIN Games g ON p.gameid = g.gameid
                    GROUP BY p.playerid
                );""";
        Connection conn = getConnection();
        try (PreparedStatement creationStatement = conn.prepareStatement(sqlStatement)) {
            creationStatement.execute();
        } catch (SQLException ex) {
            Wathe.LOGGER.error("SQLException when trying to initialize PlayerStats view", ex);
        }
    }

    protected void initializeGamesTable() {
        String sqlStatement = "CREATE TABLE IF NOT EXISTS Games (gameid VARCHAR(40) PRIMARY KEY, winners VARCHAR(40), status VARCHAR(40), gametime INTEGER)";
        Connection conn = getConnection();
        try (PreparedStatement creationStatement = conn.prepareStatement(sqlStatement)) {
            creationStatement.execute();
        } catch (SQLException ex) {
            Wathe.LOGGER.error("SQLException when trying to initialize Games", ex);
        }
    }

    protected void initializePlayedInTable() {
        // SQLite doesn't support BOOLEAN (https://sqlite.org/quirks.html#no_separate_boolean_datatype)
        // use integer
        String sqlStatement = "CREATE TABLE IF NOT EXISTS PlayedIn (playerid VARCHAR(40), gameid VARCHAR(40), rolename VARCHAR(40), died INTEGER, leftgame INTEGER, PRIMARY KEY (playerid, gameid), CONSTRAINT fk_Games FOREIGN KEY (gameid) REFERENCES Games(gameid))";
        Connection conn = getConnection();
        try (PreparedStatement creationStatement = conn.prepareStatement(sqlStatement)) {
            creationStatement.execute();
        } catch (SQLException ex) {
            Wathe.LOGGER.error("SQLException when trying to initialize Games", ex);
        }
    }
}
