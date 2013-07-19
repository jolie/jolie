/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi <famontesi@gmail.com>          *
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


type CommandExecutionRequest:string { // The command to execute
	.waitFor?:int // 1 if the command is to be waited for, 0 otherwise
	.args[0,*]:string // Arguments to be passed to the command
	.workingDirectory?:string // Working directory for the process to execute (default: current directory)
	.stdOutConsoleEnable?: bool // if true standard output is redirected to console
}

type CommandExecutionResult:any { // Can be string or void
	.exitCode?:int // The exit code of the executed command
	.stderr?:string // The standard error output of the executed command
}

interface ExecInterface {
	RequestResponse:
		exec(CommandExecutionRequest)(CommandExecutionResult)
}

outputPort Exec {
	Interfaces: ExecInterface
}

embedded {
Java:
	"joliex.util.ExecService" in Exec
}
