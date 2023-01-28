package com.cxj.plugins.wuziqi

import java.awt.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import javax.imageio.ImageIO
import javax.imageio.stream.FileImageInputStream
import javax.swing.JPanel

class Board(private val cols: Int, private val rows: Int) {
    private val left = 80
    private val top = 40
    private val width = 680
    private val height = 680
    private val b = 40
    val _board: Array<IntArray> = Array(rows){ IntArray(cols) }

    fun canPlace(col: String, row: Int): Boolean {
        val nCol = Global.m[col]!!
        return _board[row][nCol] == 0
    }

    fun ban33(p: Point): Boolean {
        return false
    }

    fun ban44(p: Point): Boolean {
        return false
    }

    fun banLong(p: Point): Boolean {
        return false
    }

    fun isBan(p: Point): Boolean {
        return ban33(p) || ban44(p) || banLong(p)
    }

    fun place(col: Int, row: Int, who: Int) {
        _board[row][col] = who
    }

    private fun inRange(col: Int, row: Int): Boolean {
        return col in 0 until cols && row in 0 until rows
    }

    private fun forward(col: Int, row: Int, who: Int, dx: Int, dy: Int): Int {
        return if(!inRange(col+dx, row+dy) || _board[row+dy][col+dx] != who) {
            0
        } else {
            1 + forward(col+dx, row+dy, who, dx, dy)
        }
    }

    fun isWin(p: Point): Boolean {
        val who = p.who
        val col = p.x
        val row = p.y
        // 上下
        var count = 1
        count += forward(col, row, who, 0, -1)
        count += forward(col, row, who, 0, 1)
        if(count >= 5) return true
        // 左右
        count = 1
        count += forward(col, row, who, -1, 0)
        count += forward(col, row, who, 1, 0)
        if(count >= 5) return true
        // 左上 右下
        count = 1
        count += forward(col, row, who, -1, -1)
        count += forward(col, row, who, 1, 1)
        if(count >= 5) return true
        // 右上 左下
        count = 1
        count += forward(col, row, who, 1, -1)
        count += forward(col, row, who, -1, 1)
        if(count >= 5) return true
        return false
    }

    private fun paintBoard2(g: Graphics2D): Graphics2D {
        return ImageIO.read(Board::class.java.classLoader.getResourceAsStream("board.jpeg")).createGraphics()
    }

    private fun paintBoard(g: Graphics2D): Graphics2D {
        // 画背景
        g.background = Color.WHITE
        g.clearRect(0, 0, width, height)

//        g.drawImage(ImageIO.read(Board::class.java.classLoader.getResourceAsStream("mu3.jpeg")), 0, 0, width, height, null)
        g.color = Color.BLACK
        val f = g.font
        val nf = Font(f.name, f.style, 30)
        g.font = nf
        val stroke = BasicStroke(3F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.stroke = stroke
        // 画线
        for(i in 0 until cols) {
            for(j in 0 until rows) {
                g.drawLine(left, top + j * b, left + b * (cols - 1), top + j * b)
                g.drawLine(left + i * b, top, left + i * b, top + b * (rows - 1))
            }
        }
        // 标注
        for(i in rows-1 downTo  0) {
            g.drawString(String.format("%2s", "${i+1}"), left - (1.5 * b).toInt(), top + b * i + (g.font.size) / 2)
        }
        for(j in 0 until cols) {
            g.drawString("${'A' + j}", left + b * j - 8, top + b * (rows - 1) + (1.5 * b).toInt())
        }
        return g
    }

    private fun paintChequer(g: Graphics2D): Graphics2D {
        _board.forEachIndexed { i, e->
            e.forEachIndexed { j, p->
                if(p == 1) {
                    g.color = Color.BLACK
                    g.fillOval(left + b * j - b / 2, top + b * i - b / 2, b, b)
                } else if (p == 2) {
                    g.color = Color.BLACK
                    g.drawOval(left + b * j - b / 2, top + b * i - b / 2, b, b)
                    g.color = Color.WHITE
                    g.fillOval(left + b * j - b / 2, top + b * i - b / 2, b, b)
                }
            }
        }
        return g
    }

    private fun paintFive(g: Graphics2D) {
        g.color = Color.BLACK
        g.fillOval(left + b * 7 - b / 4, top + b * 7 - b / 4, b/2, b/2)
        g.fillOval(left + b * 3 - b / 4, top + b * 3 - b / 4, b/2, b/2)
        g.fillOval(left + b * 11 - b / 4, top + b * 3 - b / 4, b/2, b/2)
        g.fillOval(left + b * 3 - b / 4, top + b * 11 - b / 4, b/2, b/2)
        g.fillOval(left + b * 11 - b / 4, top + b * 11 - b / 4, b/2, b/2)
    }

    fun paintLast(p: Point?, g: Graphics2D) {
        if(p != null) {
            if(p.who == 1) {
                g.color = Color.WHITE
            } else {
                g.color = Color.BLACK
            }
            g.drawLine(left + b * p.x - b / 4, top + b * p.y, left + b * p.x + b / 4, top + b * p.y)
            g.drawLine(left + b * p.x, top + b * p.y - b / 4, left + b * p.x, top + b * p.y + b / 4)
        }
    }

    fun patinBoardWithChequer(p: Point?): ByteArrayOutputStream {
//        val pic =
//            ImageIO.read(Board::class.java.classLoader.getResourceAsStream("board.png"))
        val pic = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        var g = pic.createGraphics()
        g = paintBoard(g)
        paintFive(g)
        paintChequer(g)
        paintLast(p, g)
        val os = ByteArrayOutputStream()
//        val h = 35
//        for(i in 0 until width) {
//            for(j in 0 until height) {
//                val old = pic.getRGB(i, j)
//                val nc = Color(old)
//                var nr = nc.red + h
//                if(nr > 255) nr = 255
//                var ng = nc.green + h
//                if(ng > 255) ng = 255
//                var nb = nc.blue + h
//                if(nb > 255) nb = 255
//                if(nr < 0) nr = 0
//                if(ng < 0) ng = 0
//                if(nb < 0) nb = 0
//                pic.setRGB(i, j, Color(nr, ng, nb).rgb)
//            }
//        }
        ImageIO.write(pic, "PNG", os)
        return os
    }

    fun addComputer(level: Int = 0) {

    }
}