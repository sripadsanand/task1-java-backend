package com.kaiburr.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import com.kaiburr.demo.model.Task;
import com.kaiburr.demo.model.TaskExecution;
import com.kaiburr.demo.repository.TaskRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;



@GetMapping
public ResponseEntity<?> getTasks(@RequestParam(required = false) String id) {
    if (id == null) {
        return ResponseEntity.ok(taskRepository.findAll());
    }

    return taskRepository.findById(id)
            .<ResponseEntity<?>>map(task -> ResponseEntity.ok().body(task))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: 404, Task not found"));
}



    @PutMapping
    public ResponseEntity<?> addTask(@RequestBody Task task) {
        if (isUnsafeCommand(task.getCommand())) {
            return ResponseEntity.badRequest().body("Unsafe command detected");
        }
        task.setTaskExecutions(new ArrayList<>());
        return ResponseEntity.ok(taskRepository.save(task));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable String id) {
        if (!taskRepository.existsById(id)) return ResponseEntity.status(404).body("Not found");
        taskRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }

    @GetMapping("/search")
    public ResponseEntity<?> findByName(@RequestParam String name) {
        List<Task> tasks = taskRepository.findByNameContainingIgnoreCase(name);
        return tasks.isEmpty() ?
                ResponseEntity.status(404).body("No tasks found") :
                ResponseEntity.ok(tasks);
    }

    @PutMapping("/{id}/execute")
    public ResponseEntity<?> executeTask(@PathVariable String id) {
        Optional<Task> optionalTask = taskRepository.findById(id);
        if (!optionalTask.isPresent()) return ResponseEntity.status(404).body("Task not found");

        Task task = optionalTask.get();
        if (isUnsafeCommand(task.getCommand())) return ResponseEntity.badRequest().body("Unsafe command");

        Date start = new Date();
        String output;
        try {
            Process process = Runtime.getRuntime().exec(task.getCommand());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line).append("\n");
            process.waitFor();
            output = result.toString();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Execution error: " + e.getMessage());
        }
        Date end = new Date();

        TaskExecution exec = new TaskExecution(start, end, output);
        task.getTaskExecutions().add(exec);
        taskRepository.save(task);

        return ResponseEntity.ok(exec);
    }

    private boolean isUnsafeCommand(String cmd) {
        String[] blacklisted = {"rm", "shutdown", "reboot", "kill", ":", "&", ";", "|", ">", "<"};
        return Arrays.stream(blacklisted).anyMatch(cmd::contains);
    }
}

