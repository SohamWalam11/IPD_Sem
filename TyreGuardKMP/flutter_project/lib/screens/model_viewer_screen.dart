import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';
import '../services/model_viewer_service.dart';
import '../config/theme_config.dart';

class ModelViewerScreen extends StatefulWidget {
  final String modelUrl;
  final String tirePosition;

  const ModelViewerScreen({
    Key? key,
    required this.modelUrl,
    required this.tirePosition,
  }) : super(key: key);

  @override
  State<ModelViewerScreen> createState() => _ModelViewerScreenState();
}

class _ModelViewerScreenState extends State<ModelViewerScreen> {
  late WebViewController _webViewController;
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _initializeWebView();
  }

  void _initializeWebView() {
    _webViewController = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setNavigationDelegate(
        NavigationDelegate(
          onPageStarted: (String url) {
            setState(() => _isLoading = true);
          },
          onPageFinished: (String url) {
            setState(() => _isLoading = false);
          },
          onWebResourceError: (WebResourceError error) {
            setState(() {
              _isLoading = false;
              _error = error.description;
            });
          },
        ),
      );

    _loadModel();
  }

  void _loadModel() {
    try {
      final modelViewerService = ModelViewerService();
      final htmlContent = modelViewerService.generateModelViewerHTML(widget.modelUrl);
      
      _webViewController.loadHtmlString(htmlContent);
    } catch (e) {
      setState(() {
        _isLoading = false;
        _error = e.toString();
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('${widget.tirePosition} - 3D Model'),
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadModel,
          ),
        ],
      ),
      body: Stack(
        children: [
          if (_error == null)
            WebViewWidget(controller: _webViewController)
          else
            _buildErrorWidget(),
          if (_isLoading)
            _buildLoadingWidget(),
        ],
      ),
    );
  }

  Widget _buildLoadingWidget() {
    return Container(
      color: Colors.black.withOpacity(0.3),
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const CircularProgressIndicator(
              valueColor: AlwaysStoppedAnimation<Color>(
                ThemeConfig.primaryColor,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Loading 3D Model...',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorWidget() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.error_outline,
              size: 64,
              color: ThemeConfig.errorColor,
            ),
            const SizedBox(height: 16),
            Text(
              'Failed to Load Model',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 8),
            Text(
              _error ?? 'An unknown error occurred',
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: () {
                setState(() => _error = null);
                _loadModel();
              },
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
            ),
            const SizedBox(height: 12),
            TextButton.icon(
              onPressed: () => Navigator.pop(context),
              icon: const Icon(Icons.arrow_back),
              label: const Text('Go Back'),
            ),
          ],
        ),
      ),
    );
  }
}
