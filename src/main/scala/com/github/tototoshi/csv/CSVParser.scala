/*
* Copyright 2013 Toshiyuki Takahashi
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.github.tototoshi.csv

object CSVParser {

  abstract sealed trait State
  case object Start extends State
  case object Field extends State
  case object Delimiter extends State
  case object End extends State
  case object QuoteStart extends State
  case object QuoteEnd extends State
  case object QuotedField extends State

  /**
   * {{{
   * scala> com.github.tototoshi.csv.CSVParser.parse("a,b,c", '\\', ',', '"')
   * res0: Option[List[String]] = Some(List(a, b, c))
   *
   * scala> com.github.tototoshi.csv.CSVParser.parse("\"a\",\"b\",\"c\"", '\\', ',', '"')
   * res1: Option[List[String]] = Some(List(a, b, c))
   * }}}
   */
  def parse(input: String, escapeChar: Char, delimiter: Char, quoteChar: Char): Option[List[String]] = {
    val buf: Array[Char] = input.toCharArray
    var fields: Vector[String] = Vector()
    var field = new StringBuilder
    var state: State = Start
    var pos = 0
    val buflen = buf.length

    while (state != End && pos < buflen) {
      val c = buf(pos)
      state match {
        case Start => {
          c match {
            case `quoteChar` => {
              state = QuoteStart
              pos += 1
            }
            case `delimiter` => {
              fields :+= field.toString
              field = new StringBuilder
              state = Delimiter
              pos += 1
            }
            case '\n' | '\u2028' | '\u2029' | '\u0085' => {
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case '\r' => {
              if (pos + 1 < buflen && buf(1) == '\n') {
                pos += 1
              }
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case x => {
              field += x
              state = Field
              pos += 1
            }
          }
        }
        case Delimiter => {
          c match {
            case `quoteChar` => {
              state = QuoteStart
              pos += 1
            }
            case `delimiter` => {
              fields :+= field.toString
              field = new StringBuilder
              state = Delimiter
              pos += 1
            }
            case '\n' | '\u2028' | '\u2029' | '\u0085' => {
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case '\r' => {
              if (pos + 1 < buflen && buf(1) == '\n') {
                pos += 1
              }
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case x => {
              field += x
              state = Field
              pos += 1
            }
          }
        }
        case Field => {
          c match {
            case `escapeChar` => {
              if (pos + 1 < buflen) {
                if (buf(pos + 1) == escapeChar
                  || buf(pos + 1) == delimiter) {
                  field += buf(pos + 1)
                  state = Field
                  pos += 2
                } else {
                  throw new MalformedCSVException(buf.mkString)
                }
              } else {
                state = QuoteEnd
                pos += 1
              }
            }
            case `delimiter` => {
              fields :+= field.toString
              field = new StringBuilder
              state = Delimiter
              pos += 1
            }
            case '\n' | '\u2028' | '\u2029' | '\u0085' => {
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case '\r' => {
              if (pos + 1 < buflen && buf(1) == '\n') {
                pos += 1
              }
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case x => {
              field += x
              state = Field
              pos += 1
            }
          }
        }
        case QuoteStart => {
          c match {
            case `quoteChar` => {
              if (pos + 1 < buflen && buf(pos + 1) == quoteChar) {
                field += quoteChar
                state = QuotedField
                pos += 2
              } else {
                fields :+= field.toString
                field = new StringBuilder
                state = QuoteEnd
                pos += 1
              }
            }
            case x => {
              field += x
              state = QuotedField
              pos += 1
            }
          }
        }
        case QuoteEnd => {
          c match {
            case `delimiter` => {
              fields :+= field.toString
              field = new StringBuilder
              state = Delimiter
              pos += 1
            }
            case '\n' | '\u2028' | '\u2029' | '\u0085' => {
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case '\r' => {
              if (pos + 1 < buflen && buf(1) == '\n') {
                pos += 1
              }
              fields :+= field.toString
              field = new StringBuilder
              state = End
              pos += 1
            }
            case _ => {
              throw new MalformedCSVException(buf.mkString)
            }
          }
        }
        case QuotedField => {
          c match {
            case `quoteChar` => {
              if (pos + 1 < buflen && buf(pos + 1) == quoteChar) {
                field += quoteChar
                state = QuotedField
                pos += 2
              } else {
                state = QuoteEnd
                pos += 1
              }
            }
            case x => {
              field += x
              state = QuotedField
              pos += 1
            }
          }
        }
        case End => {
          sys.error("unexpected error")
        }
      }
    }
    state match {
      case Delimiter => {
        fields :+= ""
        Some(fields.toList)
      }
      case QuotedField => {
        None
      }
      case _ => {
        if (!field.isEmpty) {
          // When no crlf at end of file
          state match {
            case Field | QuoteEnd => {
              fields :+= field.toString
            }
            case _ => {
            }
          }
        }
        Some(fields.toList)
      }
    }
  }
}

class CSVParser(format: CSVFormat) {

  def parseLine(input: String): Option[List[String]] = {
    val parsedResult = CSVParser.parse(input, format.escapeChar, format.delimiter, format.quoteChar)
    if (parsedResult == Some(List("")) && format.treatEmptyLineAsNil) Some(Nil)
    else parsedResult
  }

}
