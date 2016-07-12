/***************************************************************************
 * Copyright (C) by Fabrizio Montesi                                     *
 * *
 * This program is free software; you can redistribute it and/or modify  *
 * it under the terms of the GNU Library General Public License as       *
 * published by the Free Software Foundation; either version 2 of the    *
 * License, or (at your option) any later version.                       *
 * *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 * *
 * You should have received a copy of the GNU Library General Public     *
 * License along with this program; if not, write to the                 *
 * Free Software Foundation, Inc.,                                       *
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 * *
 * For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package jolie.process;

import jolie.runtime.*;
import jolie.runtime.expression.Expression;
import jolie.util.Pair;

public class ForEachArrayProcess implements Process {

    final private VariablePath keyPath, targetPath;
    final private Process process;

    public ForEachArrayProcess(
            VariablePath keyPath,
            VariablePath targetPath,
            Process process) {
        this.keyPath = keyPath;
        this.targetPath = targetPath;
        this.process = process;
    }

    public Process clone(TransformationReason reason) {
        return new ForEachArrayProcess(
                keyPath.clone(),
                targetPath.clone(),
                process.clone(reason)
        );
    }

    public void run()
            throws FaultException, ExitingException {

        ValueVector targetVector = targetPath.getValueVector();
        int size = targetVector.size();
        int length = targetPath.path().length;
        Pair<Expression, Expression>[] path = targetPath.path();

        for (int i = 0; i < size; i++) {
            path[length - 1] = new Pair<>(path[length - 1].key(), Value.create(i));
            keyPath.makePointer(targetPath);
            process.run();
        }
    }

    public boolean isKillable() {
        return true;
    }
}
