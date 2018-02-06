package com.basic.json

import java.util.Stack
import scala.io.Source
import java.util.HashMap
import util.{Try, Success, Failure}
import Array._

object SmoothJson {

    def numCheck(str : String, dtype : String) : Boolean = {
        var c:Try[Any] = null
        var result = true
        if( "DOUBLE".equals(dtype) ) {
            result = str.contains(".")
            c = scala.util.Try(str.toDouble)
        } else if( "INT".equals(dtype) ) {
            println(str)
            c = scala.util.Try(str.toInt)
        } else if( "LONG".equals(dtype) ) {
            println(str)
            result = scala.util.Try(str.toInt).isInstanceOf[Failure[Int]]
            c = scala.util.Try(str.toLong)
        } else {
            ;
        }
        val check = c match {
            case Success(_) => true;
            case _ =>  false;
        }
        result && check
    }

    class Block(_value : String, _ctg : String) {
        var valueLong : Long = 0L
        var valueInt : Int = 0
        var valueDouble : Double = 0.0
        var valueString : String = ""
        var valueArray : Array[Block] = null
        var valueBoolean = false
        var valueMap : java.util.HashMap[String, Block] = null
        if ( _ctg == "MAP" ) {
            valueMap = new java.util.HashMap[String, Block]()
        } else if ( _ctg == "ARRAY" ) {
            valueArray = new Array[Block](0)
        } else if ( _ctg == "VALUE" ) {
            if ( _value == "true" || _value == "false" ) {
                valueBoolean = _value.toBoolean
            } else if ( numCheck(_value, "INT") ) {
                valueInt = _value.toInt
            } else if ( numCheck(_value, "DOUBLE") ) {
                valueDouble = _value.toDouble
            } else if ( numCheck(_value, "LONG") ) {
                valueLong = _value.toLong
            } else {
                valueString = _value
            }
        } else {
            ;
        }
    }

    class Level(_block : Block, _ctg : String, key : String) {
        var block : Block = _block
        var keyLeft = false
        var keyRight = false
        var crtKey : String = key
        var nextKey = ""
        var ctg = _ctg
    }

    def fatherProcess(levelStack : Stack[Level]) : Block = {
        var finalBlock : Block = null
        val crtStack = levelStack.pop()
        if ( levelStack.size() > 0 ) {
            if ( levelStack.peek().ctg == "MAP" ) {
                levelStack.peek().block.valueMap.put(crtStack.crtKey, crtStack.block)
            } else if ( levelStack.peek().ctg == "ARRAY" ) {
                levelStack.peek().block.valueArray = levelStack.peek().block.valueArray :+ crtStack.block
            } else {
                ;
            }
        } else {
            finalBlock = crtStack.block
        }
        finalBlock
    }

    def directValue(levelStack : Stack[Level], raw : String, pos : Int, valueType : String) : Int = {
        var i = pos
        var block : Block = null
        var isStr = false
        var value = ""
        var k = 0
        if ( raw.substring(i, i+1) == "\"" ) {
            k = i + 1
            isStr = true
            var nextChar = raw.substring(k, k+1)
            while ( nextChar != "\"" ) {
                value += nextChar
                k = k + 1
                nextChar = raw.substring(k, k+1)
            }
        } else {
            var nextChar = raw.substring(i, i+1)
            while ( nextChar != "," && nextChar != " " && nextChar != valueType && nextChar != "\n" && nextChar != "\r\n" ) {
                value += nextChar
                i = i + 1
                nextChar = raw.substring(i, i+1)
            }
        }
        if ( isStr ) {
            i = k
        } else {
            i = i - 1
        }
        block = new Block(value, "VALUE")
        if ( valueType == "}" ) {
            levelStack.peek().block.valueMap.put(levelStack.peek().nextKey, block)
        } else if ( valueType == "]" ) {
            levelStack.peek().block.valueArray = levelStack.peek().block.valueArray :+ block
        } else {
            println("Body closure symbol must be '}' or ']'")
            System.exit(-1)
        }
        i
    }

    var rootBlock : Block = null

    def process(raw : String) : Block = {
        var levelStack : Stack[Level] = new Stack[Level]()
        val charStart = raw.substring(0, 1)
        if ( charStart == "{" ) {
            levelStack.push(new Level(new Block(null, "MAP"), "MAP", "ROOT"))
        } else if ( charStart == "[" ) {
            levelStack.push(new Level(new Block(null, "ARRAY"), "ARRAY", "ROOT"))
        } else {
            println("Raw json stream not invalid!")
            System.exit(-1)
        }
        var i = 0
        var run = true
        var finalBlock : Block = null
        while ( i < raw.length() && run ) {
            val crtChar = raw.substring(i, i+1)
            if ( levelStack.peek().ctg == "MAP" ) {
                if ( !levelStack.peek().keyLeft && !levelStack.peek().keyRight && crtChar == "}" ) {
                    finalBlock = fatherProcess(levelStack)
                    if ( finalBlock != null ) {
                        run = false
                    }
                } else {
                    if ( levelStack.peek().keyLeft && !levelStack.peek().keyRight && crtChar != "\"") {
                        levelStack.peek().nextKey += crtChar
                    }
                    if ( crtChar == "\"" ) {
                        if ( !levelStack.peek().keyLeft ) {
                            levelStack.peek().keyLeft = true
                        } else {
                            if ( !levelStack.peek().keyRight ) {
                                levelStack.peek().keyRight = true
                                var j = i + 1
                                while ( raw.substring(j, j+1) == " " || raw.substring(j, j+1) == ":" ) {
                                    j = j + 1
                                }
                                i = j
                                if ( raw.substring(i, i+1) == "{" ) {
                                    levelStack.peek().keyLeft = false
                                    levelStack.peek().keyRight = false
                                    var nextKey = levelStack.peek().nextKey
                                    levelStack.peek().nextKey = ""
                                    levelStack.push(new Level(new Block(null, "MAP"), "MAP", nextKey))
                                } else if ( raw.substring(i, i+1) == "[" ) {
                                    levelStack.peek().keyLeft = false
                                    levelStack.peek().keyRight = false
                                    var nextKey = levelStack.peek().nextKey
                                    levelStack.peek().nextKey = ""
                                    levelStack.push(new Level(new Block(null, "ARRAY"), "ARRAY", nextKey))
                                } else {
                                    i = directValue(levelStack, raw, i, "}")
                                }
                                levelStack.peek().keyLeft = false
                                levelStack.peek().keyRight = false
                                levelStack.peek().nextKey = ""
                            }
                        }
                    }
                }
            } else if ( levelStack.peek().ctg == "ARRAY" ) {
                var j = i
                while ( raw.substring(j, j+1) == " " || raw.substring(j, j+1) == "," ) {
                    j = j + 1
                }
                i = j
                if ( raw.substring(i, i+1) == "]" ) {
                    finalBlock = fatherProcess(levelStack)
                    if ( finalBlock != null ) {
                        run = false
                    }
                } else if ( raw.substring(i, i+1) == "{" ) {
                    levelStack.push(new Level(new Block(null, "MAP"), "MAP", null))
                } else if ( raw.substring(i, i+1) == "[" ) {
                    levelStack.push(new Level(new Block(null, "ARRAY"), "ARRAY", null))
                } else {
                    i = directValue(levelStack, raw, i, "]")
                }
            }
            i = i + 1
        }
        finalBlock
    }

    def load(path : String) : Block = {
        rootBlock = process(Source.fromFile(path).mkString)
        rootBlock
    }

    def loads(raw : String) : Block = {
        rootBlock = process(raw)
        rootBlock
    }

    def getJSONObject() : Block = {
        rootBlock
    }
}
