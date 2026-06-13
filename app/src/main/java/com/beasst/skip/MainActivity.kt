package com.beasst.skip

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var display: TextView
    private var currentInput = ""
    private var operator = ""
    private var previousValue = ""
    private var shouldResetDisplay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.display)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        display = findViewById(R.id.display)

        // Number buttons
        setupNumberButton(R.id.btn_0, "0")
        setupNumberButton(R.id.btn_1, "1")
        setupNumberButton(R.id.btn_2, "2")
        setupNumberButton(R.id.btn_3, "3")
        setupNumberButton(R.id.btn_4, "4")
        setupNumberButton(R.id.btn_5, "5")
        setupNumberButton(R.id.btn_6, "6")
        setupNumberButton(R.id.btn_7, "7")
        setupNumberButton(R.id.btn_8, "8")
        setupNumberButton(R.id.btn_9, "9")
        setupNumberButton(R.id.btn_decimal, ".")

        // Operator buttons
        setupOperatorButton(R.id.btn_add, "+")
        setupOperatorButton(R.id.btn_subtract, "−")
        setupOperatorButton(R.id.btn_multiply, "×")
        setupOperatorButton(R.id.btn_divide, "÷")

        // Action buttons
        findViewById<Button>(R.id.btn_equals).setOnClickListener { calculateResult() }
        findViewById<Button>(R.id.btn_clear).setOnClickListener { clearCalculator() }
        findViewById<Button>(R.id.btn_backspace).setOnClickListener { backspace() }
        findViewById<Button>(R.id.btn_toggle_sign).setOnClickListener { toggleSign() }
    }

    private fun setupNumberButton(buttonId: Int, number: String) {
        findViewById<Button>(buttonId).setOnClickListener {
            appendNumber(number)
        }
    }

    private fun setupOperatorButton(buttonId: Int, op: String) {
        findViewById<Button>(buttonId).setOnClickListener {
            selectOperator(op)
        }
    }

    private fun appendNumber(number: String) {
        // Prevent multiple decimal points
        if (number == "." && currentInput.contains(".")) {
            return
        }

        if (shouldResetDisplay) {
            currentInput = number
            shouldResetDisplay = false
        } else {
            currentInput += number
        }

        display.text = currentInput.ifEmpty { "0" }
    }

    private fun selectOperator(op: String) {
        if (currentInput.isEmpty()) {
            return
        }

        if (previousValue.isNotEmpty() && operator.isNotEmpty()) {
            // Calculate result of previous operation before starting a new one
            calculateResult()
        }

        previousValue = currentInput
        operator = op
        currentInput = ""
        shouldResetDisplay = true
        display.text = operator
    }

    private fun calculateResult() {
        if (currentInput.isEmpty() || previousValue.isEmpty() || operator.isEmpty()) {
            return
        }

        val result = try {
            val prev = previousValue.toDouble()
            val current = currentInput.toDouble()

            when (operator) {
                "+" -> prev + current
                "−" -> prev - current
                "×" -> prev * current
                "÷" -> {
                    if (current == 0.0) {
                        display.text = "Error"
                        previousValue = ""
                        operator = ""
                        currentInput = ""
                        shouldResetDisplay = true
                        return
                    }
                    prev / current
                }
                else -> return
            }
        } catch (e: Exception) {
            display.text = "Error"
            return
        }

        // Format result to avoid excessive decimal places
        currentInput = if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            String.format("%.6f", result).trimEnd('0').trimEnd('.')
        }

        display.text = currentInput
        previousValue = ""
        operator = ""
        shouldResetDisplay = true
    }

    private fun clearCalculator() {
        currentInput = ""
        previousValue = ""
        operator = ""
        shouldResetDisplay = false
        display.text = "0"
    }

    private fun backspace() {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            display.text = currentInput.ifEmpty { "0" }
        }
    }

    private fun toggleSign() {
        if (currentInput.isEmpty()) {
            return
        }

        currentInput = if (currentInput.startsWith("-")) {
            currentInput.substring(1)
        } else {
            "-$currentInput"
        }

        display.text = currentInput
    }
}

