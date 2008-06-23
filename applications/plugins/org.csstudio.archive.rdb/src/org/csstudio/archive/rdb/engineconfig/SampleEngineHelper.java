package org.csstudio.archive.rdb.engineconfig;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.csstudio.archive.rdb.internal.RDBArchiveImpl;

/** Helper for handling SampleEngineInfo entries in RDB
 *  @author Kay Kasemir
 */
public class SampleEngineHelper
{
    final private RDBArchiveImpl archive;
    
    public SampleEngineHelper(final RDBArchiveImpl archive)
    {
        this.archive = archive;
    }
    
    /** Add sample engine info to RDB.
     *  Existing info is cleared!
     *  @param name
     *  @param description
     *  @param url
     *  @return SampleEngineInfo
     *  @throws Exception on error
     */
    public SampleEngineConfig add(final String name, final String description, final URL url)
        throws Exception
    {
        final Connection connection = archive.getRDB().getConnection();
        final SampleEngineConfig found = find(name);
        final int id;
        if (found != null)
        {   // Delete existing entries, then re-use the ID
            deleteEngineInfo(found);
            id = found.getId();
        }
        else
            id = getNextID();
        final PreparedStatement statement = connection.prepareStatement(
                archive.getSQL().smpl_eng_insert);
        try
        {
            statement.setInt(1, id);
            statement.setString(2, name);
            statement.setString(3, description);
            statement.setString(4, url.toString());
            statement.executeUpdate();
        }
        finally
        {
            statement.close();
        }
        connection.commit();
        return new SampleEngineConfig(archive, id, name, description, url);
    }

    /** Delete engine info, all the groups under it, and clear all links
     *  from channels to those groups.
     *  @param engine Engine info to remove
     *  @throws Exception on error
     */
    public void deleteEngineInfo(final SampleEngineConfig engine)
            throws Exception
    {
        // Unlink all channels from engine's groups
        final Connection connection = archive.getRDB().getConnection();
        PreparedStatement statement = connection.prepareStatement(
                    archive.getSQL().channel_clear_grp_for_engine);
        try
        {
            statement.setInt(1, engine.getId());
            statement.executeUpdate();
        }
        finally
        {
            statement.close();
        }
        // Delete all groups under engine...
        statement = connection.prepareStatement(
                archive.getSQL().chan_grp_delete_by_engine_id);
        try
        {
            statement.setInt(1, engine.getId());
            statement.executeUpdate();
        }
        finally
        {
            statement.close();
        }
        // Delete Engine entry
        statement = connection.prepareStatement(archive.getSQL().smpl_eng_delete);
        try
        {
            statement.setInt(1, engine.getId());
            statement.executeUpdate();
        }
        finally
        {
            statement.close();
        }
    }

    /** @return Next available engine ID */
    private int getNextID() throws Exception
    {
        final Statement statement = archive.getRDB().getConnection().createStatement();
        try
        {
            ResultSet res = statement.executeQuery(archive.getSQL().smpl_eng_next_id);
            if (res.next())
            {
                final int id = res.getInt(1);
                if (id > 0)
                    return id + 1;
            }
            return 1;
        }
        finally
        {
            statement.close();
        }
    }

    /** Find engine by name
     *  @param name
     *  @return SampleEngineConfig or <code>null</code>
     *  @throws Exception on error
     */
    public SampleEngineConfig find(final String name) throws Exception
    {
        final PreparedStatement statement = archive.getRDB().getConnection().prepareStatement(
                archive.getSQL().smpl_eng_sel_by_name);
        try
        {
            statement.setString(1, name);
            final ResultSet res = statement.executeQuery();
            if (res.next())
                return new SampleEngineConfig(archive, res.getInt(1), name,
                        res.getString(2), new URL(res.getString(3)));
        }
        finally
        {
            statement.close();
        }
        return null;
    }

    /** Find engine by ID
     *  @param engine_id ID
     *  @return SampleEngineConfig or <code>null</code>
     *  @throws Exception on error
     */
    public SampleEngineConfig find(final int engine_id) throws Exception
    {
        final PreparedStatement statement = archive.getRDB().getConnection().prepareStatement(
                archive.getSQL().smpl_eng_sel_by_id);
        try
        {
            statement.setInt(1, engine_id);
            final ResultSet res = statement.executeQuery();
            if (res.next())
                return new SampleEngineConfig(archive, engine_id, res.getString(1),
                        res.getString(2), new URL(res.getString(3)));
        }
        finally
        {
            statement.close();
        }
        return null;
    }
}
