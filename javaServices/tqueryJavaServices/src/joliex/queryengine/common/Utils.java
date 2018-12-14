/*
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>
 *   Copyright (C) 2018 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as
 *   published by the Free Software Foundation; either version 2 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the
 *   Free Software Foundation, Inc.,
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 *   For details about the authors of this software, see the AUTHORS file.
 */package joliex.queryengine.common;

import java.util.ArrayList;
import java.util.Arrays;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public final class Utils {

	public static ValueVector evaluatePath(Value data, String path) {
		ArrayList<String> paths = new ArrayList<>(Arrays.asList(path.split("\\.")));
		return evaluatePathRec(data, paths, ValueVector.create());
	}

	private static ValueVector evaluatePathRec(Value data, ArrayList<String> paths, ValueVector values) {
		ValueVector children = data.getChildren(paths.remove(0));

		if (paths.isEmpty()) {
			for (Value child : children) {
				if (child.hasChildren()) // for situations when data="awards.award", path="awards"
				{
					return ValueVector.create();
				} else {
					values.add(child);
				}
			}
		} else {
			children.forEach(child -> evaluatePathRec(child, paths, values));
		}

		return values;
	}

	public static Value flatChildren(Value data, String head, Value child) {
		Value dataCopy = Value.create();

		dataCopy.deepCopy(data);
		dataCopy.children().remove(head);

		ValueVector newChild = ValueVector.create();

		newChild.add(child);
		dataCopy.children().put(head, newChild);

		return dataCopy;
	}

	public static boolean isGreater(Value first, String second) {
		if (isNumeric(first.strValue()) && isNumeric(second)) {
			return first.doubleValue() > Double.parseDouble(second);
		} else {
			return first.strValue().length() > second.length();
		}
	}

	public static boolean isNumeric(String str) {
		return str.matches("-?\\d+(\\.\\d+)?");
	}

	public static class Constants {

		public static String right = "right";
		public static String left = "left";
		public static String and = "and";
		public static String or = "or";
		public static String not = "not";
		public static String equal = "equal";
		public static String greaterThen = "greaterThen";
		public static String lowerThen = "lowerThen";
		public static String exists = "exists";
		public static String path = "path";
		public static String val = "value";
	}
}


//~ Formatted by Jindent --- http://www.jindent.com
