package org.csstudio.archive.engine.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.csstudio.archive.engine.Messages;
import org.csstudio.archive.engine.model.ArchiveChannel;
import org.csstudio.archive.engine.model.ArchiveGroup;
import org.csstudio.archive.engine.model.BufferStats;
import org.csstudio.archive.engine.model.EngineModel;
import org.csstudio.archive.engine.model.SampleBuffer;

/** Provide web page with detail for one group.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class GroupResponse extends AbstractResponse
{
    /** Avoid serialization errors */
    private static final long serialVersionUID = 1L;

    GroupResponse(final EngineModel model)
    {
        super(model);
    }
    
    @Override
    protected void fillResponse(final HttpServletRequest req,
                    final HttpServletResponse resp) throws Exception
    {
        // Locate the group
        final String group_name = req.getParameter("name");
        if (group_name == null)
        {
            resp.sendError(400, "Missing group name");
            return;
        }
        final ArchiveGroup group = model.getGroup(group_name);
        if (group == null)
        {
            resp.sendError(400, "Unknown group " + group_name);
            return;
        }

        HTMLWriter html = new HTMLWriter(resp,
                        "Archive Engine Group " + group_name);
                                         
        // Basic group info
        html.openTable(2, new String[]
        {
            Messages.HTTP_Status
        });
        html.tableLine(new String[]
        {
            Messages.HTTP_State,
            group.isEnabled() ? Messages.HTTP_Enabled : Messages.HTTP_Disabled
        });
        final ArchiveChannel ena_channel = group.getEnablingChannel();
        if (ena_channel != null)
        {
            html.tableLine(new String[]
            {
                Messages.HTTP_EnablingChannel,
                HTMLWriter.makeLink("channel?name=" + ena_channel.getName(),
                         ena_channel.getName())
            });
        }
        html.closeTable();
        
        html.h2(Messages.HTTP_Channels);
                             
        // HTML Table of all channels in the group
        html.openTable(1, new String[]
        {
            Messages.HTTP_Channel,
            Messages.HTTP_Connected,
            Messages.HTTP_Mechanism,
            Messages.HTTP_LastArchivedValue,
            Messages.HTTP_QueueLen,
            Messages.HTTP_QueueAvg,
            Messages.HTTP_QueueMax,
            Messages.HTTP_QueueCapacity,
            Messages.HTTP_QueueOverruns,
        });
        final int channel_count = group.getChannelCount();
        for (int j=0; j<channel_count; ++j)
        {
            final ArchiveChannel channel = group.getChannel(j);
            final String connected = channel.isConnected() 
            ? Messages.HTTP_Connected : HTMLWriter.makeRedText(Messages.HTTP_Disconnected);
            final SampleBuffer buffer = channel.getSampleBuffer();
            final BufferStats stats = buffer.getBufferStats();
            final int overrun_count = stats.getOverruns();
            String overruns = Integer.toString(overrun_count);
            if (overrun_count > 0)
                overruns = HTMLWriter.makeRedText(overruns);
            html.tableLine(new String[]
            {
                HTMLWriter.makeLink("channel?name=" + channel.getName(), channel.getName()),
                connected,
                channel.getMechanism(),
                channel.getValue(),
                Integer.toString(buffer.getQueueSize()),
                String.format("%.1f", stats.getAverageSize()),
                Integer.toString(stats.getMaxSize()),
                Integer.toString(buffer.getCapacity()),
                overruns,
            });
        }
        html.closeTable();
            
        html.close();
    }
}
