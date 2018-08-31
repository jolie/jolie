/********************************************************************************
  *   Copyright (C) 2017 by Martin Møller Andersen <maan511@student.sdu.dk>     *
  *   Copyright (C) 2017 by Fabrizio Montesi <famontesi@gmail.com>              *
  *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
  *                                                                             *
  *   This program is free software; you can redistribute it and/or modify      *
  *   it under the terms of the GNU Library General Public License as           *
  *   published by the Free Software Foundation; either version 2 of the        *
  *   License, or (at your option) any later version.                           *
  *                                                                             *
  *   This program is distributed in the hope that it will be useful,           *
  *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
  *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
  *   GNU General Public License for more details.                              *
  *                                                                             *
  *   You should have received a copy of the GNU Library General Public         *
  *   License along with this program; if not, write to the                     *
  *   Free Software Foundation, Inc.,                                           *
  *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
  *                                                                             *
  *   For details about the authors of this software, see the AUTHORS file.     *
  *******************************************************************************/

package jolie.net;


import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;

class EncodedJsonRpcContent {
    
    private final ByteBuf content;
    private final Charset charset;
    
    public EncodedJsonRpcContent( ByteBuf content, Charset charset ){
        this.content = content;
        this.charset = charset;
    }

    public ByteBuf getContent() {
        return content;
    }

    public Charset getCharset() {
        return charset;
    }
    
    public String text() {
        return content.toString( charset );
    }
    
}
