/*******************************************************************************
 * Copyright (c) 2017 Science & Technology Facilities Council.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.server.json;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.csstudio.archive.engine.Messages;
import org.csstudio.archive.engine.model.ArchiveChannel;
import org.csstudio.archive.engine.model.ArchiveGroup;
import org.csstudio.archive.engine.model.EngineModel;
import org.csstudio.archive.engine.server.AbstractResponse;

/** Provide web page with list of disconnected channels in JSON
 *  @author Dominic Oram
 */
@SuppressWarnings("nls")
public class JSONDisconnectedResponse extends AbstractResponse
{
    public JSONDisconnectedResponse(final EngineModel model)
    {
        super(model);
    }

    @Override
    public void fillResponse(final HttpServletRequest req,
                    final HttpServletResponse resp) throws Exception
    {
        final JSONWriter json = new JSONWriter(resp);
        json.writeObjectKey(Messages.HTTP_DisconnectedTitle);
        json.openList();

        boolean isFirst = true;
        final int group_count = model.getGroupCount();
        for (int i=0; i<group_count; ++i)
        {
            final ArchiveGroup group = model.getGroup(i);
            final int channel_count = group.getChannelCount();
            for (int j=0; j<channel_count; ++j)
            {
                final ArchiveChannel channel = group.getChannel(j);
                if (channel.isConnected())
                    continue;

                // Only print the list seperator on later entries
                if (!isFirst) {
                    json.listSeperator();
                } else {
                    isFirst = false;
                }
                json.openObject();
                json.writeObjectEntry(Messages.HTTP_Channel, channel.getName());
                json.writeObjectEntry(Messages.HTTP_Group, group.getName());
                json.closeObject();

            }
        }
        json.closeList();
        json.closeObject();
    }
}
