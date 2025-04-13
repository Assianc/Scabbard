package com.assiance.scabbard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.assiance.scabbard.databinding.ActivityMainBinding
import java.util.*

class MainActivity : StartActivity() {
    private lateinit var binding: ActivityMainBinding
    private var players: MutableList<String> =
        mutableListOf("示例元素1", "示例元素2", "示例元素3", "示例元素4")
    private var classificationType = 2 // 默认二分类
    private var selectedPlayers: MutableList<String> = mutableListOf()
    private var isTeamsAllocated = false // 标志是否已经进行过分类
    private lateinit var selectedPlayersText: TextView
    private lateinit var addPlayerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedPlayersText = findViewById(R.id.selectedPlayersText)

        // 初始化 ActivityResultLauncher
        addPlayerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { data ->
                    data.getStringArrayListExtra("allPlayers")?.let {
                        players.clear()
                        players.addAll(it)
                    }
                    data.getStringArrayListExtra("selectedPlayers")?.let {
                        selectedPlayers = ArrayList(it)
                        // 更新已选择元素的显示
                        updateSelectedPlayersText()
                    }
                }
            }
        }

        setupClassificationSpinner()
        setupButtons()
    }

    private fun setupClassificationSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this, R.array.classification_array, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.classificationSpinner.adapter = adapter
        // 设置Spinner的OnItemSelectedListener
        binding.classificationSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    classificationType = when (parent.getItemAtPosition(position).toString()) {
                        "二分类" -> 2
                        "三分类" -> 3
                        "四分类" -> 4
                        else -> 2
                    }
                    // 更新分类视图
                    updateClassificationViews()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    classificationType = 2
                }
            }
    }

    private fun setupButtons() {
        binding.addPlayerButton.setOnClickListener {
            val intent = Intent(this, PlayerSelectionActivity::class.java)
            intent.putStringArrayListExtra("currentPlayers", ArrayList(players))
            addPlayerLauncher.launch(intent)
        }
        binding.allocateButton.setOnClickListener {
            allocateTeams(selectedPlayers)
        }
    }

    private fun formatPlayerList(team: List<String>): String = team.joinToString(", ")

    @SuppressLint("SetTextI18n")
    private fun allocateTeams(selectedPlayers: List<String>) {
        if (selectedPlayers.isEmpty()) {
            Toast.makeText(this, "请选择玩家进行分类", Toast.LENGTH_SHORT).show()
            return
        }
        // 转换为可变列表并打乱顺序
        val shuffledPlayers = selectedPlayers.toMutableList().apply { shuffle() }
        resetTeamTexts()
        when (classificationType) {
            2 -> {
                val halfSize = shuffledPlayers.size / 2
                val teamA = shuffledPlayers.subList(0, halfSize)
                val teamB = shuffledPlayers.subList(halfSize, shuffledPlayers.size)
                binding.teamAText.text = "区域 A: ${formatPlayerList(teamA)}"
                binding.teamBText.text = "区域 B: ${formatPlayerList(teamB)}"
            }

            3 -> {
                val (teamA, teamB, teamC) = shuffledPlayers.partitionToThree()
                binding.teamAText.text = "区域 A: ${formatPlayerList(teamA)}"
                binding.teamBText.text = "区域 B: ${formatPlayerList(teamB)}"
                binding.teamCText.text = "区域 C: ${formatPlayerList(teamC)}"
                binding.teamCText.visibility = View.VISIBLE
            }

            4 -> {
                val (teamA, teamB, teamC, teamD) = shuffledPlayers.partitionToFour()
                binding.teamAText.text = "区域 A: ${formatPlayerList(teamA)}"
                binding.teamBText.text = "区域 B: ${formatPlayerList(teamB)}"
                binding.teamCText.text = "区域 C: ${formatPlayerList(teamC)}"
                binding.teamDText.text = "区域 D: ${formatPlayerList(teamD)}"
                binding.teamCText.visibility = View.VISIBLE
                binding.teamDText.visibility = View.VISIBLE
            }
        }
        isTeamsAllocated = true
    }

    private fun updateClassificationViews() {
        if (!isTeamsAllocated) {
            binding.teamCText.visibility = if (classificationType > 2) View.VISIBLE else View.GONE
            binding.teamDText.visibility = if (classificationType > 3) View.VISIBLE else View.GONE
        }
    }

    private fun resetTeamTexts() {
        binding.teamAText.text = ""
        binding.teamBText.text = ""
        binding.teamCText.text = ""
        binding.teamDText.text = ""
        binding.teamCText.visibility = View.GONE
        binding.teamDText.visibility = View.GONE
    }

    // 三分法
    private fun List<String>.partitionToThree(): Triple<List<String>, List<String>, List<String>> {
        val sizeThird = size / 3
        val teamA = take(sizeThird + if (size % 3 > 0) 1 else 0)
        val teamB = drop(teamA.size).take(sizeThird + if ((size - teamA.size) % 2 > 0) 1 else 0)
        val teamC = drop(teamA.size + teamB.size)
        return Triple(teamA, teamB, teamC)
    }

    // 四分法
    private fun List<String>.partitionToFour(): Quadruple<List<String>, List<String>, List<String>, List<String>> {
        val sizeFourth = size / 4
        val teamA = take(sizeFourth + if (size % 4 > 0) 1 else 0)
        val teamB = drop(teamA.size).take(sizeFourth + if ((size - teamA.size) % 3 > 0) 1 else 0)
        val teamC =
            drop(teamA.size + teamB.size).take(sizeFourth + if ((size - teamA.size - teamB.size) % 2 > 0) 1 else 0)
        val teamD = drop(teamA.size + teamB.size + teamC.size)
        return Quadruple(teamA, teamB, teamC, teamD)
    }

    data class Quadruple<out A, out B, out C, out D>(
        val first: A, val second: B, val third: C, val fourth: D
    )

    @SuppressLint("SetTextI18n")
    private fun updateSelectedPlayersText() {
        if (selectedPlayers.isEmpty()) {
            selectedPlayersText.text = "未选择任何元素"
        } else {
            selectedPlayersText.text = "已选择: ${selectedPlayers.joinToString(", ")}"
        }
    }
}