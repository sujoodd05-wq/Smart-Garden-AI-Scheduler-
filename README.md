Smart Garden AI Scheduler
An intelligent plant watering scheduling system built entirely in Java. This application integrates Machine Learning for decision-making and Stochastic Optimization for efficient path planning.
Project Overview
The system is designed to automate the decision of "when" and "how" to water a garden. It consists of two primary engines developed from scratch without the use of external AI libraries:
Perceptron Classifier: A machine learning model that predicts whether a plant requires watering based on its features.
Simulated Annealing Optimizer: A metaheuristic algorithm that finds the most efficient watering sequence to minimize walking distance and penalties.
Technical Features
Library-Free Implementation: Both the Perceptron and Simulated Annealing algorithms are implemented using pure Java logic, demonstrating a deep understanding of the underlying mathematics.
Dynamic Data Integration: Supports uploading custom datasets via CSV files with automated data normalization and Fisher-Yates shuffling for unbiased training.
Customizable Training: Users can configure training parameters such as epochs, learning rates, and Train/Test split ratios (e.g., 70/30 or 80/20).
Interactive Visualization:
Real-time Learning Curves for monitoring Accuracy and Loss during training.
Animated Pathfinding to visualize the Simulated Annealing optimization process step-by-step.
Graphical Garden View for spatial plant management.
Core Logic
1. Classification (Perceptron)
The model classifies plants based on three features: Soil Moisture, Last Watered Time, and Plant Type. It utilizes the Perceptron Learning Rule to adjust weights and bias, achieving optimal linear separation of data.
2. Path Optimization (Simulated Annealing)
The optimizer solves a variation of the Traveling Salesman Problem (TSP). It employs a multi-objective cost function:
Cost = (Missed Plants Penalty) + (Total Euclidean Distance) + (Extra Watering Penalty)
It use the Metropolis Criterion to escape local optima and settle on a global minimum cost.
Tech Stack
Language: Java (JDK 17+)
UI Framework: Java Swing and AWT
Data Format: CSV
How to Use
Run the MainGUI class.
Add plants manually in the Garden tab or upload a CSV file in the Perceptron tab.
Train the model to see the Learning Curve.
Go to the SA Optimizer tab, set the number of plants to visit, and run the optimizer.
Use "Show SA Steps" to view the animated path development.
