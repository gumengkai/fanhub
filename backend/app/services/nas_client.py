import os
import json


def test_nas_connection(nas_config):
    """Test connection to a NAS server."""
    if not nas_config:
        return {
            'success': False,
            'error': 'No NAS configuration provided'
        }

    protocol = nas_config.get('protocol', 'smb')
    host = nas_config.get('host')
    port = nas_config.get('port')
    username = nas_config.get('username')
    password = nas_config.get('password')
    share = nas_config.get('share')
    path = nas_config.get('path', '')

    if not host:
        return {
            'success': False,
            'error': 'NAS host is required'
        }

    try:
        if protocol == 'smb':
            return test_smb_connection(host, port, username, password, share, path)
        elif protocol == 'nfs':
            return test_nfs_connection(host, port, path)
        else:
            return {
                'success': False,
                'error': f'Unsupported protocol: {protocol}'
            }
    except Exception as e:
        return {
            'success': False,
            'error': str(e)
        }


def test_smb_connection(host, port, username, password, share, path):
    """Test SMB/CIFS connection."""
    try:
        # Try to import smbprotocol for testing
        try:
            from smbprotocol.connection import Connection
            from smbprotocol.session import Session

            port = port or 445

            conn = Connection(uuid.uuid4().hex, host, port)
            conn.connect()

            if username:
                session = Session(conn, username, password)
                session.connect()
            else:
                session = Session(conn, '', '')
                session.connect()

            # Try to connect to share if specified
            if share:
                from smbprotocol.tree import TreeConnect
                tree = TreeConnect(session, f"\\\\{host}\\{share}")
                tree.connect()
                tree.disconnect()

            session.disconnect()
            conn.disconnect()

            return {
                'success': True,
                'protocol': 'smb',
                'host': host,
                'port': port,
                'share': share
            }

        except ImportError:
            # Fallback: try using smbclient CLI
            import subprocess

            cmd = ['smbclient', '-L', host, '-N']
            if username:
                cmd = ['smbclient', '-L', host, '-U', username]

            result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)

            if result.returncode == 0:
                return {
                    'success': True,
                    'protocol': 'smb',
                    'host': host,
                    'note': 'CLI fallback used'
                }
            else:
                return {
                    'success': False,
                    'error': result.stderr or 'SMB connection failed'
                }

    except Exception as e:
        return {
            'success': False,
            'error': f'SMB connection error: {str(e)}'
        }


def test_nfs_connection(host, port, path):
    """Test NFS connection."""
    try:
        # Try using showmount to check NFS exports
        import subprocess

        result = subprocess.run(
            ['showmount', '-e', host],
            capture_output=True,
            text=True,
            timeout=30
        )

        if result.returncode == 0:
            return {
                'success': True,
                'protocol': 'nfs',
                'host': host,
                'exports': result.stdout.strip().split('\n')[1:]  # Skip header
            }
        else:
            return {
                'success': False,
                'error': result.stderr or 'NFS connection failed'
            }

    except Exception as e:
        return {
            'success': False,
            'error': f'NFS connection error: {str(e)}'
        }


def mount_nas_share(source, mount_point):
    """Mount a NAS share to a local mount point."""
    if source.type != 'nas':
        return {'success': False, 'error': 'Source is not a NAS type'}

    nas_config = source.nas_config or {}
    protocol = nas_config.get('protocol', 'smb')
    host = nas_config.get('host')
    share = nas_config.get('share')
    username = nas_config.get('username')
    password = nas_config.get('password')

    if not host or not share:
        return {'success': False, 'error': 'Host and share are required'}

    try:
        # Create mount point if it doesn't exist
        os.makedirs(mount_point, exist_ok=True)

        if protocol == 'smb':
            # Mount SMB share
            mount_path = f"//{host}/{share}"
            cmd = ['mount', '-t', 'cifs', mount_path, mount_point]

            if username:
                cmd.extend(['-o', f'username={username},password={password}'])
            else:
                cmd.extend(['-o', 'guest'])

        elif protocol == 'nfs':
            # Mount NFS share
            mount_path = f"{host}:{share}"
            cmd = ['mount', '-t', 'nfs', mount_path, mount_point]

        else:
            return {'success': False, 'error': f'Unsupported protocol: {protocol}'}

        result = subprocess.run(cmd, capture_output=True, text=True)

        if result.returncode == 0:
            return {
                'success': True,
                'mount_point': mount_point,
                'protocol': protocol,
                'source': mount_path
            }
        else:
            return {
                'success': False,
                'error': result.stderr
            }

    except Exception as e:
        return {
            'success': False,
            'error': str(e)
        }


def unmount_nas_share(mount_point):
    """Unmount a NAS share."""
    try:
        import subprocess

        result = subprocess.run(
            ['umount', mount_point],
            capture_output=True,
            text=True
        )

        if result.returncode == 0:
            return {'success': True}
        else:
            return {'success': False, 'error': result.stderr}

    except Exception as e:
        return {'success': False, 'error': str(e)}
