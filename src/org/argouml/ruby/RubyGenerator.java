/* $Id$
 *****************************************************************************
 * Copyright (c) 2009 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    mvw
 *****************************************************************************
 *
 * Some portions of this file was previously release using the BSD License:
 */

// Copyright (c) 2006-2008 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies. This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason. IN NO EVENT SHALL THE
// UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

package org.argouml.ruby;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.argouml.model.Model;
import org.argouml.uml.generator.CodeGenerator;
import org.argouml.uml.generator.SourceUnit;

/**
 * Generate Ruby code.
 * 
 * @author Jacek Bojarski
 * 
 */
public class RubyGenerator implements CodeGenerator {
    /**
     * TODO: Where should we put the files?
     */
    private String whereToGenerate = "/tmp/generatedRuby";
    private String ext = ".rb";

    /**
     * Do the actual generation for an object.
     * 
     * @param o
     *            The object.
     * @return a SourceUnit.
     */
    private SourceUnit generateForObject(Object o) {
        if (Model.getFacade().isAClass(o))
            return generateClass(o);

        return null;
    }

    /**
     * Generate Code for class.
     * 
     * @param o The object.
     * @return a SourceUnit.
     */
    private SourceUnit generateClass(Object o) {
        // TODO:use string buffer!?
        String className = capitalize(Model.getFacade().getName(o));
        String src = "class " + className;

        // generalization:
        String baseClass = generateGeneralization(Model.getFacade()
                .getGeneralizations(o));
        if (!baseClass.equals("")) {
            src += " < " + capitalize(baseClass);
        }
        src += "\n";

        src += getAttributes(o);
        src += getOperations(o);
        src += "\nend";
        return new SourceUnit(
                capitalize(Model.getFacade().getName(o)) + ext,
                whereToGenerate, src);

    }

    /**
     * Generate code for generalizations of class.
     * 
     * @param generalizations The list of generalizations.
     * @return code fo generalization.
     */
    private String generateGeneralization(Collection generalizations) {
        if (generalizations == null) {
            return "";
        }
        String baseClass = "";
        Iterator iter = generalizations.iterator();
        while (iter.hasNext()) {
            Object generalization = iter.next();
            Object generalizableElement = Model.getFacade().getGeneral(
                    generalization);
            if (generalizableElement != null) {
                baseClass = generateClassifierName(generalizableElement);
                break;
            }
        }
        return baseClass;
    }

    public String generateClassifierName(Object cls) {
        if (cls == null) {
            return "";
        }
        return Model.getFacade().getName(cls);
    }

    private String getAttributes(Object o) {
        String attrsSrc = "";
        Collection attrs = Model.getFacade().getAttributes(o);
        // get classifier attributes
        for (Iterator i = attrs.iterator(); i.hasNext();) {          
            Object attr = i.next();
            String varType = "@";
            String defaultValue = "";
            String attrName = Model.getFacade().getName(attr);
            // is static?
            if (Model.getFacade().isStatic(attr)) {
                varType += "@";
                defaultValue = "=nil";
            }    		
            // Get initial value of attribut
            Object init = (Model.getFacade().getInitialValue(attr));
            if (init != null) {
                defaultValue = "="
                        + ((String) (Model.getFacade().getBody(init))).trim();
                // need more elegant solution ;)
            }
            attrsSrc += varType + attrName + defaultValue + "\n";
        }        
        attrsSrc += "\n";
        return attrsSrc;   	
    }

    private String getOperations(Object o) {
        String opersSrc = "";
        String protectedList = "";
        String privateList = "";
        Collection opers = Model.getFacade().getOperations(o);
        for (Iterator i = opers.iterator(); i.hasNext();) {
            Object oper = i.next();
            String operName = Model.getFacade().getName(oper);
            opersSrc += "def " + operName;
            // get params
            String params = getOperationsParams(oper);
            if (!params.equals("")) {
                opersSrc += "(" + params + ")";
            }
            opersSrc += "\nend\n";
            // get visibility of operation
            if (Model.getFacade().isProtected(oper)) {
                protectedList += ":" + operName + ",";
            }
            else if (Model.getFacade().isPrivate(oper)) {
                privateList += ":" + operName + ",";
            }
        }        
        // write visibility of all operations
        if (!protectedList.equals("")) {
            opersSrc += "protected "
                    + protectedList.substring(0, protectedList.length() - 1)
                    + "\n";
        }
        if (!privateList.equals("")) {
            opersSrc += "private "
                    + privateList.substring(0, privateList.length() - 1) + "\n";
        }
        return opersSrc;
    }	


    private String getOperationsParams(Object o) {
        String paramsSrc = "";
        Collection params = Model.getFacade().getParameters(o);
        // get operation params
        for (Iterator i = params.iterator(); i.hasNext();) {          
            Object param = i.next();    		 
            String defaultValue = "";
            String paramName = Model.getFacade().getName(param);
            // skip return
            if (paramName.equals("return")) {
                continue;
            }
            // Get default value of param
            Object init = (Model.getFacade().getDefaultValue(param));
            if (init != null) {
                defaultValue = "="
                        + ((String) (Model.getFacade().getBody(init))).trim();
            }
            paramsSrc += paramName + defaultValue + ",";
        }
        if (!paramsSrc.equals("")) {
            paramsSrc = paramsSrc.substring(0, paramsSrc.length() - 1);
        }
        return paramsSrc;  
    }

    private String capitalize(String str) {
        if (str.length() > 1) {
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        } else {
            return str.toUpperCase();
        }
    }

    /*
     * @see org.argouml.uml.generator.CodeGenerator#generate(java.util.Collection,
     *      boolean)
     */
    public Collection generate(Collection elements, boolean deps) {
        Collection res = new ArrayList();

        for (Iterator i = elements.iterator(); i.hasNext();) {
            Object o = i.next();
            SourceUnit su = generateForObject(o);
            if (su != null) {
                res.add(su);
            }
        }
        return res;
    }

    /*
     * @see org.argouml.uml.generator.CodeGenerator#generateFiles(java.util.Collection,
     *      java.lang.String, boolean)
     */
    public Collection generateFiles(Collection elements, String path,
            boolean deps) {
        Collection res = new ArrayList();

        for (Iterator i = elements.iterator(); i.hasNext();) {
            Object o = i.next();
            SourceUnit su = generateForObject(o);
            if (su != null) {
                su.setBasePath(path);
                File file = new File(su.getFullName());
                try {
                    OutputStream os = new FileOutputStream(file);
                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    osw.write(su.getContent());
                    osw.close();
                } catch (IOException e) {					
                    // TODO: The file could not be created.
                }
                res.add(file.getName());
            }
        }
        return res;
    }

    /*
     * @see org.argouml.uml.generator.CodeGenerator#generateFileList(java.util.Collection,
     *      boolean)
     */
    public Collection generateFileList(Collection elements, boolean deps) {
        Collection res = new ArrayList();

        for (Iterator i = elements.iterator(); i.hasNext();) {
            Object o = i.next();
            SourceUnit su = generateForObject(o);
            System.out.println(su.getFullName());
            File file = new File(su.getFullName());
            res.add(file.getName());
        }
        return res;
    }

}
