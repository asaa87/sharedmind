/*FreeMind - A Program for creating and viewing Mindmaps
 *Copyright (C) 2000-2001  Joerg Mueller <joergmueller@bigfoot.com>
 *See COPYING for Details
 *
 *This program is free software; you can redistribute it and/or
 *modify it under the terms of the GNU General Public License
 *as published by the Free Software Foundation; either version 2
 *of the License, or (at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program; if not, write to the Free Software
 *Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
/* $Id: Mode.java,v 1.8.18.2.2.4 2008/04/11 16:58:31 christianfoltin Exp $ */

package freemind.modes;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import freemind.controller.Controller;
import freemind.main.XMLParseException;

public interface Mode {

    public void init(Controller c);
    public String toString();
    public void activate();
    public void restore(String restorable) throws FileNotFoundException, XMLParseException, MalformedURLException, IOException, URISyntaxException ;
    /** Creates a new mode controller and returns it.
     */
    public ModeController createModeController();
    /** This modeController is only used, when no map is opened.
     */
    public ModeController getDefaultModeController();
    public Controller getController();
}
