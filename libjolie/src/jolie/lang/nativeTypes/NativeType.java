/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>               *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package jolie.lang.nativeTypes;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Fabrizio Montesi
 */
public class NativeType {

	private final NativeTypeEnum type;

	public NativeType(NativeTypeEnum type) {
		this.type = type;
	}

	public NativeTypeEnum getType() {
		return type;
	}

	public enum NativeTypeEnum {

		//UNDEFINED( "undefined" ),
		STRING("string"),
		INT("int"),
		LONG("long"),
		BOOL("bool"),
		DOUBLE("double"),
		VOID("void"),
		RAW("raw"),
		ANY("any");

		public static Map<String, NativeTypeEnum> getIdMap() {
			return idMap;
		}

		final static Map<String, NativeTypeEnum> idMap = new HashMap<>();

		static {
			for (NativeTypeEnum type : NativeTypeEnum.values()) {
				idMap.put(type.id(), type);
			}
		}

		private final String id;

		private NativeTypeEnum(String id) {
			this.id = id;
		}

		public String id() {
			return id;
		}

		public static NativeTypeEnum fromString(String id) {
			return idMap.get(id);
		}

		public static boolean isNativeTypeKeyword(String id) {
			return idMap.containsKey(id);
		}

	}
}
