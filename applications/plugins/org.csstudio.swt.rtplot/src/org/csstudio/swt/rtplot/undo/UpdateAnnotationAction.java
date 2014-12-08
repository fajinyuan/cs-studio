/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.rtplot.undo;

import org.csstudio.swt.rtplot.Messages;
import org.csstudio.swt.rtplot.internal.AnnotationImpl;
import org.csstudio.swt.rtplot.internal.Plot;

/** Action to update Annotation
 *  @author Kay Kasemir
 */
public class UpdateAnnotationAction<XTYPE extends Comparable<XTYPE>> implements UndoableAction
{
    final private Plot<XTYPE> plot;
    final AnnotationImpl<XTYPE> annotation;
    final private XTYPE start_pos, end_pos;
    final private double start_val, end_val;

    public UpdateAnnotationAction(final Plot<XTYPE> plot, final AnnotationImpl<XTYPE> annotation,
            final XTYPE start_pos, final double start_val,
            final XTYPE end_pos, final double end_val)
    {
        this.plot = plot;
        this.annotation = annotation;
        this.start_pos = start_pos;
        this.start_val = start_val;
        this.end_pos = end_pos;
        this.end_val = end_val;
    }

    @Override
    public void run()
    {
        plot.updateAnnotation(annotation, end_pos, end_val);
    }

    @Override
    public void undo()
    {
        plot.updateAnnotation(annotation, start_pos, start_val);
    }

    @Override
    public String toString()
    {
        return Messages.MoveAnnotation;
    }
}
