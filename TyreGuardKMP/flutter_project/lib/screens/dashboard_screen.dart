import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';
import '../config/theme_config.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({Key? key}) : super(key: key);

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  int _selectedIndex = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('TyreGuard Dashboard'),
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.notifications),
            onPressed: () {
              // Navigate to notifications
            },
          ),
          IconButton(
            icon: const Icon(Icons.person),
            onPressed: () {
              // Navigate to profile
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Welcome Section
              Text(
                'Welcome, ${context.read<AuthService>().currentUser?.firstName}!',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 8),
              Text(
                'Monitor your tire health',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
              const SizedBox(height: 24),
              // Quick Actions
              Row(
                children: [
                  Expanded(
                    child: _buildActionCard(
                      icon: Icons.camera_alt,
                      label: 'Capture Tire',
                      onTap: () {
                        // Navigate to camera
                      },
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _buildActionCard(
                      icon: Icons.location_on,
                      label: 'Find Service',
                      onTap: () {
                        // Navigate to service centers
                      },
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              // Tire Overview Section
              Text(
                'Your Tires',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 12),
              _buildTireOverview(),
              const SizedBox(height: 24),
              // Recent Analysis
              Text(
                'Recent Analysis',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 12),
              _buildRecentAnalysis(),
            ],
          ),
        ),
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _selectedIndex,
        onTap: (index) {
          setState(() => _selectedIndex = index);
        },
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.home),
            label: 'Home',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.history),
            label: 'History',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.settings),
            label: 'Settings',
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          // Navigate to camera
        },
        child: const Icon(Icons.camera_alt),
      ),
    );
  }

  Widget _buildActionCard({
    required IconData icon,
    required String label,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            children: [
              Icon(
                icon,
                size: 32,
                color: ThemeConfig.primaryColor,
              ),
              const SizedBox(height: 8),
              Text(
                label,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTireOverview() {
    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      mainAxisSpacing: 12,
      crossAxisSpacing: 12,
      children: [
        _buildTireCard('Front Left', 85),
        _buildTireCard('Front Right', 72),
        _buildTireCard('Rear Left', 65),
        _buildTireCard('Rear Right', 78),
      ],
    );
  }

  Widget _buildTireCard(String position, int healthScore) {
    Color healthColor;
    if (healthScore >= 70) {
      healthColor = ThemeConfig.healthyColor;
    } else if (healthScore >= 40) {
      healthColor = ThemeConfig.cautionColor;
    } else {
      healthColor = ThemeConfig.criticalColor;
    }

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.tire_repair,
              size: 32,
              color: healthColor,
            ),
            const SizedBox(height: 8),
            Text(
              position,
              style: Theme.of(context).textTheme.bodySmall,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              '$healthScore%',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: healthColor,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRecentAnalysis() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            ListTile(
              leading: const Icon(Icons.check_circle, color: ThemeConfig.healthyColor),
              title: const Text('Front Left Tire'),
              subtitle: const Text('Analyzed 2 hours ago'),
              trailing: const Icon(Icons.arrow_forward),
              onTap: () {
                // Navigate to analysis details
              },
            ),
            const Divider(),
            ListTile(
              leading: const Icon(Icons.warning, color: ThemeConfig.cautionColor),
              title: const Text('Rear Right Tire'),
              subtitle: const Text('Analyzed 1 day ago'),
              trailing: const Icon(Icons.arrow_forward),
              onTap: () {
                // Navigate to analysis details
              },
            ),
          ],
        ),
      ),
    );
  }
}
