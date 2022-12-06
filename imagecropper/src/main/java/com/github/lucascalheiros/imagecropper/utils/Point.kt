package com.github.lucascalheiros.imagecropper.utils

data class Point(val x: Float, val y: Float)

operator fun Point.minus(other: Point): Point {
    return Point(x - other.x, y - other.y)
}

operator fun Point.plus(other: Point): Point {
    return Point(x + other.x, y + other.y)
}

operator fun Point.div(divisor: Float): Point {
    return Point(x / divisor, y / divisor)
}

fun Pair<Float, Float>.toPoint(): Point {
    return Point(first, second)
}